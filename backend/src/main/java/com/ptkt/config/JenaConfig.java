package com.ptkt.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.Dataset;
import org.apache.jena.tdb2.TDB2Factory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Slf4j
@Configuration
public class JenaConfig {

    @Value("${jena.tdb2.location:./data/jena-tdb}")
    private String tdbLocation;

    @Bean(destroyMethod = "close")
    public Dataset jenaDataset() {
        File dir = new File(tdbLocation);
        if (!dir.exists()) {
            dir.mkdirs();
            log.info("[Jena] TDB2 디렉토리 생성: {}", dir.getAbsolutePath());
        }
        Dataset ds = TDB2Factory.connectDataset(tdbLocation);
        log.info("[Jena] TDB2 데이터셋 초기화 완료: {}", dir.getAbsolutePath());
        return ds;
    }
}
