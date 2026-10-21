#include "SwordBridge.h"
#include "GalarEncounters.h"
#include <cstdlib>
#include <ctime>

// Pirated Version bridge implementation.
// Uses the encounter tables in GalarEncounters.cpp (pkNX-derived).
// Replace stub bodies with calls into RecompSwordC objects once linked:
// see CMakeLists.txt for the recomp/ drop-in instructions.

static bool g_initialized = false;

static int rand_range(int lo, int hi) {
    return lo + std::rand() % (hi - lo + 1);
}

extern "C" {

int sword_bridge_init(void) {
    if (g_initialized) return 1;
    std::srand(static_cast<unsigned>(std::time(nullptr)));
    // TODO: call RecompSwordC init when linked
    g_initialized = true;
    return 1;
}

int sword_bridge_pick_encounter(int area_id, int* out_dex_id, int* out_level) {
    if (!out_dex_id || !out_level) return -1;
    const EncounterArea* area = galar_find_area(area_id);
    if (!area || area->symbol_count == 0) return 1;

    // Pick a random subtable (weather/time variant — first one for now)
    const SubTable* tbl = &area->symbol[0];
    if (tbl->slot_count == 0) return 1;

    // Probability-weighted roll
    int roll = rand_range(1, 100);
    int acc = 0;
    for (int i = 0; i < tbl->slot_count; ++i) {
        acc += tbl->slots[i].probability;
        if (roll <= acc) {
            *out_dex_id = tbl->slots[i].species;
            *out_level  = rand_range(tbl->level_min, tbl->level_max);
            return 0;
        }
    }
    // Fallback to last slot
    *out_dex_id = tbl->slots[tbl->slot_count - 1].species;
    *out_level  = rand_range(tbl->level_min, tbl->level_max);
    return 0;
}

void sword_bridge_shutdown(void) {
    g_initialized = false;
}

// ── JNI entry points (built when -DSWORD_BRIDGE_JNI=ON) ───────────────────
#ifdef SWORD_BRIDGE_JNI
#include <jni.h>

JNIEXPORT jintArray JNICALL
Java_de_fiereu_openmmo_server_game_services_sword_SwordShieldService_bridgePickEncounter(
    JNIEnv* env, jobject /*self*/, jint areaId)
{
    int dex = 0, level = 0;
    sword_bridge_pick_encounter(static_cast<int>(areaId), &dex, &level);
    jintArray arr = env->NewIntArray(2);
    jint buf[2] = {dex, level};
    env->SetIntArrayRegion(arr, 0, 2, buf);
    return arr;
}

JNIEXPORT jboolean JNICALL
Java_de_fiereu_openmmo_server_game_services_sword_SwordShieldService_bridgeInit(
    JNIEnv* /*env*/, jobject /*self*/)
{
    return sword_bridge_init() ? JNI_TRUE : JNI_FALSE;
}
#endif

} // extern "C"
