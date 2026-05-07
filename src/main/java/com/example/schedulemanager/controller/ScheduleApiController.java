package com.example.schedulemanager.controller;

import com.example.schedulemanager.dto.ScheduleRequest;
import com.example.schedulemanager.dto.ScheduleCsvImportResult;
import com.example.schedulemanager.dto.ScheduleJoinRequestCreateRequest;
import com.example.schedulemanager.model.ScheduleItem;
import com.example.schedulemanager.service.ScheduleService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleApiController {
    private final ScheduleService scheduleService;

    public ScheduleApiController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<ScheduleItem> findByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserDetails userDetails) {
        return scheduleService.getByDate(date, userDetails.getUsername());
    }

    @GetMapping("/month")
    public List<ScheduleItem> findByMonth(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @AuthenticationPrincipal UserDetails userDetails) {
        return scheduleService.getByMonth(year, month, userDetails.getUsername());
    }

    @GetMapping("/title-suggestions")
    public List<String> titleSuggestions(
            @RequestParam(value = "limit", required = false) Integer limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        return scheduleService.getTitleSuggestions(userDetails.getUsername(), limit);
    }

    @GetMapping("/reminders")
    public List<java.util.Map<String, Object>> reminders(
            @RequestParam(value = "windowMinutes", defaultValue = "60") int windowMinutes,
            @AuthenticationPrincipal UserDetails userDetails) {
        return scheduleService.findDueSoonReminders(userDetails.getUsername(), windowMinutes);
    }

    @PostMapping
    public ResponseEntity<ScheduleItem> create(
            @RequestBody ScheduleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ScheduleItem update(
            @PathVariable("id") Long id,
            @RequestBody ScheduleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return scheduleService.update(id, request, userDetails.getUsername());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        scheduleService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    public ScheduleItem complete(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return scheduleService.complete(id, userDetails.getUsername());
    }

    @DeleteMapping("/{id}/complete")
    public ScheduleItem uncomplete(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return scheduleService.uncomplete(id, userDetails.getUsername());
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Void> join(
            @PathVariable("id") Long id,
            @RequestBody(required = false) ScheduleJoinRequestCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        scheduleService.join(id, userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/join")
    public ResponseEntity<Void> cancelJoin(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        scheduleService.cancelJoin(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/join-requests/{joinRequestId}/approve")
    public ResponseEntity<Void> approveJoinRequest(
            @PathVariable("id") Long id,
            @PathVariable("joinRequestId") Long joinRequestId,
            @AuthenticationPrincipal UserDetails userDetails) {
        scheduleService.decideJoinRequest(id, joinRequestId, true, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/join-requests/{joinRequestId}/reject")
    public ResponseEntity<Void> rejectJoinRequest(
            @PathVariable("id") Long id,
            @PathVariable("joinRequestId") Long joinRequestId,
            @AuthenticationPrincipal UserDetails userDetails) {
        scheduleService.decideJoinRequest(id, joinRequestId, false, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<ScheduleItem> share(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.shareToFriends(id, userDetails.getUsername()));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ScheduleCsvImportResult importCsv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        return scheduleService.importFromCsv(file, userDetails.getUsername());
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] body = scheduleService.buildCsvTemplate().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"schedule-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}
