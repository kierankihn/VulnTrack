package com.vulntrack.service;

import com.vulntrack.entity.SystemSetting;
import com.vulntrack.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    public static final String NVD_API_KEY = "nvd.api.key";

    private final SystemSettingRepository repo;

    @Transactional(readOnly = true)
    public String get(String key, String fallback) {
        return repo.findById(key).map(SystemSetting::getValue).orElse(fallback);
    }

    @Transactional
    public void set(String key, String value) {
        SystemSetting s = repo.findById(key).orElseGet(() -> {
            SystemSetting fresh = new SystemSetting();
            fresh.setKey(key);
            return fresh;
        });
        s.setValue(value);
        s.setUpdatedAt(LocalDateTime.now());
        repo.save(s);
    }
}
