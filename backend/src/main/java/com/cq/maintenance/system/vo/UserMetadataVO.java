package com.cq.maintenance.system.vo;
import java.util.List;
public record UserMetadataVO(List<Option> roles,List<Option> skills,List<Option> teams,List<Option> workshops){public record Option(Long id,String code,String name){}}
