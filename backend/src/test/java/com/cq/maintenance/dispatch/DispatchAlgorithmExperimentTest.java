package com.cq.maintenance.dispatch;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cq.maintenance.dispatch.mapper.DispatchMapper;
import com.cq.maintenance.dispatch.service.DispatchRecommendationService;
import com.cq.maintenance.dispatch.vo.DispatchContext;
import com.cq.maintenance.dispatch.vo.DispatchEngineerRow;
import com.cq.maintenance.dispatch.vo.DispatchRecommendationVO;
import com.cq.maintenance.dispatch.vo.SkillMatchRow;
import com.cq.maintenance.security.LoginUser;
import com.cq.maintenance.workorder.entity.WorkOrderStatus;
import com.cq.maintenance.workorder.entity.WorkOrderType;
import com.cq.maintenance.workorder.service.WorkOrderScopeService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Reproducible comparison used by the stage-9 algorithm experiment report. */
class DispatchAlgorithmExperimentTest {
    private static final List<Engineer> ENGINEERS = List.of(
        new Engineer(1, 1, 1, Set.of(1L, 2L), 0),
        new Engineer(2, 1, 1, Set.of(1L), 1),
        new Engineer(3, 2, 1, Set.of(3L), 0),
        new Engineer(4, 2, 1, Set.of(1L, 3L), 2),
        new Engineer(5, 3, 2, Set.of(2L), 0),
        new Engineer(6, 3, 2, Set.of(3L), 1)
    );
    private static final Map<Long, List<SkillMatchRow>> REQUIREMENTS = Map.of(
        101L, skills(1, 2),
        102L, skills(1, 3),
        103L, skills(2)
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void weightedScoringIsComparedWithLowestLoadBaselineOnFixedData() {
        List<Job> jobs = new ArrayList<>();
        for (int round = 0; round < 8; round++) {
            jobs.add(new Job(101, 1, 1));
            jobs.add(new Job(102, 2, 1));
            jobs.add(new Job(103, 3, 2));
        }

        Metrics weighted = runWeighted(jobs);
        Metrics baseline = runLowestLoad(jobs);
        System.out.printf(
            "STAGE9_EXPERIMENT weighted=%s baseline=%s%n",
            weighted,
            baseline
        );

        assertTrue(weighted.averageSkillMatch > baseline.averageSkillMatch);
        assertTrue(weighted.teamMatchRate > baseline.teamMatchRate);
        assertTrue(weighted.areaMatchRate > baseline.areaMatchRate);
        assertTrue(weighted.averageCompositeScore > baseline.averageCompositeScore);
    }

    private Metrics runWeighted(List<Job> jobs) {
        Map<Long, Integer> loads = initialLoads();
        DispatchMapper mapper = mock(DispatchMapper.class);
        WorkOrderScopeService scopes = mock(WorkOrderScopeService.class);
        DispatchRecommendationService service = new DispatchRecommendationService(
            mapper,
            scopes,
            new DispatchProperties()
        );
        loginAsAdmin();
        Map<Long, Job> jobsById = new HashMap<>();
        when(mapper.findContext(anyLong())).thenAnswer(invocation -> {
            long id = invocation.getArgument(0);
            Job job = jobsById.get(id);
            return new DispatchContext(id, "EXP-" + id, WorkOrderType.REPAIR,
                WorkOrderStatus.PENDING_ASSIGN, job.typeId, job.teamId, job.workshopId);
        });
        when(mapper.findRequiredSkills(anyLong())).thenAnswer(invocation ->
            REQUIREMENTS.get(invocation.<Long>getArgument(0)));
        when(mapper.findEligibleEngineers()).thenAnswer(invocation -> ENGINEERS.stream()
            .map(engineer -> new DispatchEngineerRow(engineer.id, "工程师" + engineer.id,
                engineer.teamId, "班组" + engineer.teamId, engineer.workshopId, loads.get(engineer.id)))
            .toList());
        when(mapper.findEngineerSkills(anyLong())).thenAnswer(invocation -> {
            Engineer engineer = engineer(invocation.getArgument(0));
            return engineer.skills.stream().sorted().map(id -> new SkillMatchRow(id, "技能" + id)).toList();
        });

        List<Choice> choices = new ArrayList<>();
        for (int i = 0; i < jobs.size(); i++) {
            long workOrderId = i + 1L;
            Job job = jobs.get(i);
            jobsById.put(workOrderId, job);
            DispatchRecommendationVO selected = service.recommend(workOrderId).get(0);
            choices.add(new Choice(job, engineer(selected.engineerId()), loads.get(selected.engineerId())));
            loads.compute(selected.engineerId(), (id, count) -> count + 1);
        }
        return metrics(choices, loads);
    }

    private Metrics runLowestLoad(List<Job> jobs) {
        Map<Long, Integer> loads = initialLoads();
        List<Choice> choices = new ArrayList<>();
        for (Job job : jobs) {
            Engineer selected = ENGINEERS.stream()
                .min(Comparator.comparingInt((Engineer value) -> loads.get(value.id)).thenComparingLong(value -> value.id))
                .orElseThrow();
            choices.add(new Choice(job, selected, loads.get(selected.id)));
            loads.compute(selected.id, (id, count) -> count + 1);
        }
        return metrics(choices, loads);
    }

    private Metrics metrics(List<Choice> choices, Map<Long, Integer> finalLoads) {
        double skill = choices.stream().mapToDouble(choice -> {
            Set<Long> required = REQUIREMENTS.get(choice.job.typeId).stream()
                .map(SkillMatchRow::skillId).collect(java.util.stream.Collectors.toSet());
            long matched = choice.engineer.skills.stream().filter(required::contains).count();
            return (double) matched / required.size();
        }).average().orElse(0);
        double team = choices.stream().filter(choice -> choice.job.teamId == choice.engineer.teamId).count()
            / (double) choices.size();
        double area = choices.stream().filter(choice -> choice.job.workshopId == choice.engineer.workshopId).count()
            / (double) choices.size();
        double composite = choices.stream().mapToDouble(choice -> {
            Set<Long> required = REQUIREMENTS.get(choice.job.typeId).stream()
                .map(SkillMatchRow::skillId).collect(java.util.stream.Collectors.toSet());
            double skillScore = choice.engineer.skills.stream().filter(required::contains).count() / (double) required.size();
            double loadScore = 1.0 / (1 + choice.loadAtAssignment);
            double teamScore = choice.job.teamId == choice.engineer.teamId ? 1 : 0;
            double areaScore = choice.job.workshopId == choice.engineer.workshopId ? 1 : 0;
            return 0.40 * skillScore + 0.30 * loadScore + 0.20 * teamScore + 0.10 * areaScore;
        }).average().orElse(0);
        double mean = finalLoads.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = finalLoads.values().stream().mapToDouble(load -> Math.pow(load - mean, 2)).average().orElse(0);
        double coefficientOfVariation = Math.sqrt(variance) / mean;
        int loadRange = finalLoads.values().stream().mapToInt(Integer::intValue).max().orElse(0)
            - finalLoads.values().stream().mapToInt(Integer::intValue).min().orElse(0);
        return new Metrics(choices.size(), skill, team, area, composite, coefficientOfVariation, loadRange);
    }

    private static List<SkillMatchRow> skills(long... ids) {
        return java.util.Arrays.stream(ids).mapToObj(id -> new SkillMatchRow(id, "技能" + id)).toList();
    }

    private Map<Long, Integer> initialLoads() {
        Map<Long, Integer> loads = new HashMap<>();
        ENGINEERS.forEach(engineer -> loads.put(engineer.id, engineer.initialLoad));
        return loads;
    }

    private Engineer engineer(long id) {
        return ENGINEERS.stream().filter(value -> value.id == id).findFirst().orElseThrow();
    }

    private void loginAsAdmin() {
        LoginUser user = new LoginUser(99L, "experiment", "实验管理员", "ENABLED",
            List.of("ADMIN"), List.of("workorder:assign"), List.of(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(user, null, List.of())
        );
    }

    private record Engineer(long id, long teamId, long workshopId, Set<Long> skills, int initialLoad) {}
    private record Job(long typeId, long teamId, long workshopId) {}
    private record Choice(Job job, Engineer engineer, int loadAtAssignment) {}
    private record Metrics(int sampleSize, double averageSkillMatch, double teamMatchRate,
                           double areaMatchRate, double averageCompositeScore,
                           double finalLoadCoefficientOfVariation, int finalLoadRange) {}
}
