# MSA 기반 채팅 애플리케이션(리팩토링)

## 1. 개요
기존에 개발했던 모놀리식 구조의 채팅 백엔드 서버를 MSA 아키텍처로 재구성하고 있습니다.

이전에는 핵심 메시지 브로커로 RabbitMQ를 사용했지만, 대용량 트래픽 처리 성능을 고려해 Kafka로 전환했습니다.

또한, 기존에는 Redis를 메시지 저장소로 활용했으나, 재구성 이후에는 MySQL을 채팅 메시지의 주요 저장소로 사용하고, Redis는 사용자 온·오프라인 상태 관리에 활용됩니다.

## 2. 역할
- 개인 프로젝트(팀 프로젝트 기능 리팩토링)
- MSA 아키텍처 전환 및 채팅 서버 확장
- 채팅 데이터베이스 전환
- RabbitMQ -> Kafka 전환
- 트랜잭션 로그 테일링 패턴 도입

## 3. 기간
- 25.08.01 ~ 25.09.30

## 4. 시스템 구성도
<img width="1065" height="586" alt="image" src="https://github.com/user-attachments/assets/606fc0e2-572c-4b71-8601-b57ff9da156d" />

## 5. 기술 스택
- Spring Boot
- Java
- Spring Cloud
- MySQL
- Redis
- Kafka
- Debezium Connector
- Jmeter
- Keycloak

## Quick Jump
- [팀 프로젝트로 만든 애플리케이션 Repository](https://github.com/nicehcy2/yeongkkuel-server)
- [모놀리식 채팅 애플리케이션 리팩토링 Repository](https://github.com/nicehcy2/Login-Websocket-STOPM)
