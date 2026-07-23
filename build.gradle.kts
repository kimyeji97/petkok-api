plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.diffplug.spotless") version "7.0.2"
    checkstyle
    jacoco
}

group = "com.petkok"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web / JPA / Security / Validation
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // DB / Migration
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // JWT (jjwt 0.12.x)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Cloudflare R2 (S3 compatible)
    implementation(platform("software.amazon.awssdk:bom:2.28.29"))
    implementation("software.amazon.awssdk:s3")

    // spring-java-utility 이식 유틸리티에서 사용
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("com.google.guava:guava:33.3.1-jre")
    implementation("org.apache.commons:commons-compress:1.27.1")

    // Lombok — @Slf4j 로깅(로거 필드 log → checkstyle ConstantName 충돌 회피) + 보일러플레이트 제거
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // QueryDSL — timeline union 등 복잡 쿼리 도입 시 활성화 (현재는 미사용)
    // implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    // annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    // annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    // annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

// ─── Spotless (google-java-format) ──────────────────────────
spotless {
    java {
        googleJavaFormat("1.22.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// ─── Checkstyle ─────────────────────────────────────────────
checkstyle {
    toolVersion = "10.17.0"
    configFile = file("config/checkstyle/checkstyle.xml")
    // 게이트 분리:
    //   - 기본(로컬·lefthook): maxWarnings 무한 → 경고만, 차단 안 함
    //   - CI(-PciStrict): maxWarnings=0 → 경고 1건이라도 task fail = 머지 게이트
    maxWarnings = if (project.hasProperty("ciStrict")) 0 else Int.MAX_VALUE
}

// ─── JaCoCo (측정만 — 게이트 없음) ──────────────────────────
jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    dependsOn(tasks.test)
}

// ─── 테스트 설정 ─────────────────────────────────────────────
tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}
