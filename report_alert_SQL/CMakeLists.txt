cmake_minimum_required(VERSION 3.25)
project(report_alert_SQL)

set(CMAKE_CXX_STANDARD 23)

# Define the executable
add_executable(report_alert_SQL main.cpp)

# Find the pqxx and pq libraries
find_library(PQXX_LIB pqxx)
find_library(PQ_LIB pq)
find_package(CURL REQUIRED)
find_package(dpp CONFIG REQUIRED)


target_include_directories(report_alert_SQL PRIVATE ${dpp_INCLUDE_DIRS})

# Link the libraries to your executable
target_link_libraries(report_alert_SQL ${PQXX_LIB} ${PQ_LIB} ${CURL_LIBRARIES} dpp::dpp)

