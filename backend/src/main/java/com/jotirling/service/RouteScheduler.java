package com.jotirling.service;
import java.time.*;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
@Component public class RouteScheduler {
 private final DeliveryReminderService service; public RouteScheduler(DeliveryReminderService s){service=s;}
 @Scheduled(fixedDelay=60000) public void check(){if(LocalTime.now(ZoneId.of("Asia/Kolkata")).isBefore(LocalTime.of(8,0)))return;service.remind();}
}
