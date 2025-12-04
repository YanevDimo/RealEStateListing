@echo off
echo Starting Kafka and Zookeeper...
docker-compose up -d zookeeper kafka
echo.
echo Waiting for Kafka to be ready...
timeout /t 10 /nobreak >nul
echo.
echo Checking Kafka status...
docker ps | findstr /i "kafka zookeeper"
echo.
echo Kafka should now be running on localhost:9092
echo You can now start your application.

