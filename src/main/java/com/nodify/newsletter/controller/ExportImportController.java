package com.nodify.newsletter.controller;

import com.nodify.newsletter.service.ExportImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Controller
public class ExportImportController {

    @Autowired
    private ExportImportService exportImportService;

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> export() throws IOException {
        byte[] data = exportImportService.exportAll();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=newsletter-backup.json");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new InputStreamResource(new ByteArrayInputStream(data)));
    }

    @PostMapping("/import")
    public String importData(@RequestParam("file") MultipartFile file) throws IOException {
        exportImportService.importAll(file.getInputStream());
        return "redirect:/";
    }
}