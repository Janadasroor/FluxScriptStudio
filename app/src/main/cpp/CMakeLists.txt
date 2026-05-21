# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name.
project("fluxscriptstudio")

# Define paths for FluxScript release 0.1.0
set(FLUX_INCLUDE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/include)
set(FLUX_LIB_DIR ${CMAKE_CURRENT_SOURCE_DIR}/lib/${ANDROID_ABI})
set(FLUX_SO ${FLUX_LIB_DIR}/libFluxScript.so)

# Detect if the FluxScript library is present to allow building even without binaries
if(EXISTS "${FLUX_SO}")
    message(STATUS "FluxScript library found at ${FLUX_SO}")
    add_library(flux_compiler SHARED IMPORTED)
    set_target_properties(flux_compiler PROPERTIES IMPORTED_LOCATION ${FLUX_SO})
    set(HAS_FLUX 1)
else()
    message(WARNING "FluxScript library NOT found at ${FLUX_SO}. Building in Mock Mode.")
    set(HAS_FLUX 0)
endif()

# Creates and names your JNI library.
add_library(${CMAKE_PROJECT_NAME} SHARED
    fluxscriptstudio.cpp)

# Include the headers
target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE ${FLUX_INCLUDE_DIR})

# Conditional linking and definition
if(HAS_FLUX)
    target_link_libraries(${CMAKE_PROJECT_NAME}
        flux_compiler
        android
        log)
    target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE HAS_FLUX_COMPILER)
else()
    target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log)
endif()
