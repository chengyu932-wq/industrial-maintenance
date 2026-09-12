package com.cq.maintenance.workorder.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BusinessNumberService {
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    public String next(String prefix){return prefix+LocalDateTime.now().format(FORMAT)+UUID.randomUUID().toString().substring(0,8).toUpperCase();}
}
