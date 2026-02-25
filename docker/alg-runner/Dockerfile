FROM eclipse-temurin:17-jre

WORKDIR /app

# 统一算法运行镜像：预置多个算法服务可执行 JAR。
COPY docs/cases/moo-nsga2-zdt1/algorithm_service/target/nsga2-zdt1-ls-1.0.0.jar /app/runners/nsga2-zdt1-ls.jar
COPY docs/cases/moo-moead-lsmop/algorithm_service/target/moead-lsmop-ls-1.0.0.jar /app/runners/moead-lsmop-ls.jar
COPY docs/cases/moo-smpso-lsmop/algorithm_service/target/smpso-lsmop-ls-1.0.0.jar /app/runners/smpso-lsmop-ls.jar
COPY docs/cases/moo-spea2-lsmop/algorithm_service/target/spea2-lsmop-ls-1.0.0.jar /app/runners/spea2-lsmop-ls.jar

COPY docker/alg-runner/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]
