cmake_minimum_required(VERSION 3.13)
project(sword_bridge CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ── DROP-IN: RecompSwordC ──────────────────────────────────────────────────
# 1. Extract RecompSwordC.zip into decomp/pokesword/recomp/
# 2. cmake -B build-ommo -DCMAKE_BUILD_TYPE=Release decomp/pokesword/pirated
# 3. cmake --build build-ommo
# 4. Copy build-ommo/libsword_bridge.so to the JVM lib path / LD_LIBRARY_PATH
# ───────────────────────────────────────────────────────────────────────────

option(SWORD_BRIDGE_JNI "Build JNI entry points for Pirated Version JVM" ON)

if(SWORD_BRIDGE_JNI)
    find_package(Java REQUIRED)
    find_package(JNI REQUIRED)
endif()

add_library(sword_bridge SHARED
    SwordBridge.cpp
    GalarEncounters.cpp
)

target_include_directories(sword_bridge PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

if(SWORD_BRIDGE_JNI)
    target_compile_definitions(sword_bridge PRIVATE SWORD_BRIDGE_JNI)
    target_include_directories(sword_bridge PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(sword_bridge PRIVATE ${JNI_LIBRARIES})
endif()

# ── Uncomment once RecompSwordC is extracted to ../recomp/ ─────────────────
# file(GLOB RECOMP_OBJS "${CMAKE_SOURCE_DIR}/../recomp/*.o")
# target_sources(sword_bridge PRIVATE ${RECOMP_OBJS})
# OR: target_link_libraries(sword_bridge PRIVATE ${CMAKE_SOURCE_DIR}/../recomp/libsword_recomp.a)
# ───────────────────────────────────────────────────────────────────────────

install(TARGETS sword_bridge LIBRARY DESTINATION lib)
