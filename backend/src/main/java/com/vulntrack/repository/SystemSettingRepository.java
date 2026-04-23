package com.vulntrack.repository;

import com.vulntrack.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
