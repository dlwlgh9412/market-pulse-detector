# Market Pulse Detector

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-6DB33F?style=flat&logo=springboot&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Cluster-DC382D?style=flat&logo=redis&logoColor=white)

## 프로젝트 소개
금융 관련 뉴스를 수집하여 LLM 모델 학습 및 분석에 활용하기 위한 **데이터 수집(Ingestion) 파이프라인**입니다.
단일 서버의 처리 한계를 극복하기 위해 **비동기 처리(Coroutines)**와 **분산 환경을 고려한 큐 시스템**을 설계하는 데 집중했습니다.

> **구현 기능:**
> * 다수의 뉴스 사이트 비동기 크롤링 및 파싱
> * Redis ZSet & Lua Script를 활용한 작업 스케줄링
> * Redis 기반의 URL 중복 제거 및 작업 스케줄링

---
프로젝트 진행 중 마주친 기술적 문제와 해결 과정을 정리했습니다

### 1. 동시성 관리
* **Problem:** 기존 스레드 풀 방식은 I/O 대기 시간이 길어질수록 리소스 낭비가 심해짐
* **Solution:** **Kotlin Coroutines & Channel** 도입
    * 생산자-소비자(Producer-Consumer) 패턴을 적용하여, 스레드 블로킹 없이 수집 작업 처리량을 개선

### 2. 분산 환경
* **Problem:** 다중 인스턴스 환경에서 스케줄러가 동시에 큐를 조회할 경우 중복 작업이 발생할 위험
* **Solution:** **Redis Lua Script** 활용
    * 조회(Read)와 상태 변경(Write)을 하나의 연산으로 처리하여 락없이 정합성 확보

### 3. 중복 제거
* **Problem:** 수집 데이터가 늘어날수록 DB(`exists`) 조회가 성능 저하를 유발
* **Solution:** **Redis BitMap 기반 필터링** 적용
    * URL을 그대로 저장하지 않고 해시 처리 후 **비트(Bit) 단위로 마킹**하여 메모리 사용량을 최소화

---

## 📂 Backend Structure
```text
com.copago.marketpulsedetector
├── core
│   ├── executor   # Jsoup & Coroutine 기반 크롤링 실행
│   ├── scheduler  # Channel 기반 작업 분배 (Producer-Consumer)
│   ├── repository
│   │   └── redis  # Lua Script & 중복 제거 구현체
│   └── service    # 도메인 로직
└── config         # Redis, WebClient 설정