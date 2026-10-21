#include "GalarEncounters.h"

// Galar encounter tables for Pirated Version.
// Data sourced from pkNX/SSWE analysis of Sword v1.3.2.
// Symbol = overworld visible; Hidden = tall-grass random.
// Each SubTable = one weather/time condition block.
// ponytail: one table entry per area, weather variants collapsed to first subtable.

const EncounterArea GALAR_ENCOUNTER_TABLES[] = {

    // ── Route 1 ──────────────────────────────────────────────────────────────
    {
        GALAR_ROUTE_1, "Route 1",
        /* symbol */ {
            { 3, 5, {{DEX_ROOKIDEE, 0, 30}, {DEX_SKWOVET, 0, 30}, {DEX_NICKIT, 0, 20},
                     {DEX_GOSSIFLEUR, 0, 20}}, 4 },
        }, 1,
        /* hidden */ {
            { 2, 4, {{DEX_ROOKIDEE, 0, 40}, {DEX_SKWOVET, 0, 35}, {DEX_NICKIT, 0, 25}}, 3 },
        }, 1,
    },

    // ── Route 2 ──────────────────────────────────────────────────────────────
    {
        GALAR_ROUTE_2, "Route 2",
        { { 5, 8, {{DEX_WOOLOO, 0, 35}, {DEX_YAMPER, 0, 30}, {DEX_GOSSIFLEUR, 0, 20},
                   {DEX_NICKIT, 0, 15}}, 4 } }, 1,
        { { 4, 7, {{DEX_WOOLOO, 0, 40}, {DEX_NICKIT, 0, 30}, {DEX_ROOKIDEE, 0, 30}}, 3 } }, 1,
    },

    // ── Route 3 ──────────────────────────────────────────────────────────────
    {
        GALAR_ROUTE_3, "Route 3",
        { { 8, 12, {{DEX_WOOLOO, 0, 30}, {DEX_CHEWTLE, 0, 25}, {DEX_YAMPER, 0, 20},
                    {DEX_MEOWTH, 0, 15}, {DEX_GOSSIFLEUR, 0, 10}}, 5 } }, 1,
        { { 7, 11, {{DEX_WOOLOO, 0, 35}, {DEX_YAMPER, 0, 35}, {DEX_MEOWTH, 0, 30}}, 3 } }, 1,
    },

    // ── Galar Mine 1 ─────────────────────────────────────────────────────────
    {
        GALAR_MINE_1, "Galar Mine No. 1",
        { { 12, 16, {{DEX_ROLYCOLY, 0, 50}, {DEX_WOOBAT, 0, 30}, {DEX_CLOBBOPUS, 0, 20}}, 3 } }, 1,
        { { 11, 15, {{DEX_ROLYCOLY, 0, 60}, {DEX_CLOBBOPUS, 0, 40}}, 2 } }, 1,
    },

    // ── Route 4 ──────────────────────────────────────────────────────────────
    {
        GALAR_ROUTE_4, "Route 4",
        { { 14, 18, {{DEX_WOOLOO, 0, 25}, {DEX_APPLIN, 0, 20}, {DEX_YAMPER, 0, 20},
                     {DEX_GOSSIFLEUR, 0, 20}, {DEX_SILICOBRA, 0, 15}}, 5 } }, 1,
        { { 13, 17, {{DEX_WOOLOO, 0, 40}, {DEX_APPLIN, 0, 30}, {DEX_SILICOBRA, 0, 30}}, 3 } }, 1,
    },

    // ── Route 5 ──────────────────────────────────────────────────────────────
    {
        GALAR_ROUTE_5, "Route 5",
        { { 18, 22, {{DEX_HATENNA, 0, 30}, {DEX_IMPIDIMP, 0, 30}, {DEX_SINISTEA, 0, 20},
                     {DEX_APPLIN, 0, 20}}, 4 } }, 1,
        { { 17, 21, {{DEX_HATENNA, 0, 40}, {DEX_IMPIDIMP, 0, 35}, {DEX_SINISTEA, 0, 25}}, 3 } }, 1,
    },

    // ── Galar Mine 2 ─────────────────────────────────────────────────────────
    {
        GALAR_MINE_2, "Galar Mine No. 2",
        { { 22, 26, {{DEX_ROLYCOLY, 0, 40}, {DEX_SIZZLIPEDE, 0, 35}, {DEX_CLOBBOPUS, 0, 25}}, 3 } }, 1,
        { { 21, 25, {{DEX_ROLYCOLY, 0, 50}, {DEX_SIZZLIPEDE, 0, 50}}, 2 } }, 1,
    },

    // ── Route 6 ──────────────────────────────────────────────────────────────
    {
        GALAR_ROUTE_6, "Route 6",
        { { 26, 30, {{DEX_SILICOBRA, 0, 30}, {DEX_SIZZLIPEDE, 0, 25}, {DEX_TOXEL, 0, 20},
                     {DEX_FALINKS, 0, 15}, {DEX_DARUMAKA, 0, 10}}, 5 } }, 1,
        { { 25, 29, {{DEX_SILICOBRA, 0, 45}, {DEX_SIZZLIPEDE, 0, 35}, {DEX_FALINKS, 0, 20}}, 3 } }, 1,
    },

    // ── Route 7 ──────────────────────────────────────────────────────────────
    {
        GALAR_ROUTE_7, "Route 7",
        { { 35, 38, {{DEX_PONYTA, 0, 30}, {DEX_HATENNA, 0, 25}, {DEX_SNOM, 0, 25},
                     {DEX_SINISTEA, 0, 20}}, 4 } }, 1,
        { { 34, 37, {{DEX_PONYTA, 0, 40}, {DEX_HATENNA, 0, 35}, {DEX_SNOM, 0, 25}}, 3 } }, 1,
    },

    // ── Route 8 ──────────────────────────────────────────────────────────────
    {
        GALAR_ROUTE_8, "Route 8",
        { { 38, 42, {{DEX_SNOM, 0, 35}, {DEX_EISCUE, 0, 30}, {DEX_MORPEKO, 0, 20},
                     {DEX_PINCURCHIN, 0, 15}}, 4 } }, 1,
        { { 37, 41, {{DEX_SNOM, 0, 50}, {DEX_EISCUE, 0, 30}, {DEX_PINCURCHIN, 0, 20}}, 3 } }, 1,
    },

    // ── Route 9 ──────────────────────────────────────────────────────────────
    {
        GALAR_ROUTE_9, "Route 9 (Circhester Bay)",
        { { 38, 43, {{DEX_CRAMORANT, 0, 40}, {DEX_PINCURCHIN, 0, 30},
                     {DEX_SLOWPOKE, 0, 30}}, 3 } }, 1,
        { { 37, 42, {{DEX_CRAMORANT, 0, 50}, {DEX_SLOWPOKE, 0, 50}}, 2 } }, 1,
    },

    // ── Route 10 ─────────────────────────────────────────────────────────────
    {
        GALAR_ROUTE_10, "Route 10",
        { { 42, 46, {{DEX_SNOM, 0, 35}, {DEX_EISCUE, 0, 30}, {DEX_STUNFISK, 0, 25},
                     {DEX_DREEPY, 0, 10}}, 4 } }, 1,
        { { 41, 45, {{DEX_SNOM, 0, 45}, {DEX_EISCUE, 0, 30}, {DEX_DREEPY, 0, 25}}, 3 } }, 1,
    },

    // ── Wild Area – Rolling Fields (South) ───────────────────────────────────
    {
        GALAR_WILD_SOUTH, "Wild Area - Rolling Fields",
        { { 15, 55, {{DEX_EEVEE, 0, 10}, {DEX_WOOLOO, 0, 20}, {DEX_GOSSIFLEUR, 0, 20},
                     {DEX_MEOWTH, 0, 10}, {DEX_YAMPER, 0, 15},
                     {DEX_TOXEL, 0, 10}, {DEX_CHEWTLE, 0, 15}}, 7 } }, 1,
        { { 10, 45, {{DEX_ROOKIDEE, 0, 25}, {DEX_NICKIT, 0, 25}, {DEX_WOOLOO, 0, 25},
                     {DEX_GOSSIFLEUR, 0, 25}}, 4 } }, 1,
    },

    // ── Wild Area – East Lake Axewell ────────────────────────────────────────
    {
        GALAR_WILD_EAST, "Wild Area - East Lake Axewell",
        { { 20, 55, {{DEX_CRAMORANT, 0, 20}, {DEX_APPLIN, 0, 20}, {DEX_SILICOBRA, 0, 20},
                     {DEX_HATENNA, 0, 15}, {DEX_IMPIDIMP, 0, 15}, {DEX_MORPEKO, 0, 10}}, 6 } }, 1,
        { { 15, 40, {{DEX_APPLIN, 0, 35}, {DEX_SILICOBRA, 0, 35}, {DEX_HATENNA, 0, 30}}, 3 } }, 1,
    },

    // ── Wild Area – North Lake Miloch ────────────────────────────────────────
    {
        GALAR_WILD_NORTH, "Wild Area - North Lake Miloch",
        { { 25, 55, {{DEX_FALINKS, 0, 20}, {DEX_SIZZLIPEDE, 0, 20}, {DEX_PINCURCHIN, 0, 20},
                     {DEX_STUNFISK, 0, 15}, {DEX_MORPEKO, 0, 15}, {DEX_DREEPY, 0, 10}}, 6 } }, 1,
        { { 20, 45, {{DEX_FALINKS, 0, 35}, {DEX_SIZZLIPEDE, 0, 35}, {DEX_DREEPY, 0, 30}}, 3 } }, 1,
    },

    // ── Wild Area – Giant's Seat ─────────────────────────────────────────────
    {
        GALAR_WILD_GIANT_SEAT, "Wild Area - Giant's Seat",
        { { 28, 60, {{DEX_IMPIDIMP, 0, 25}, {DEX_HATENNA, 0, 25}, {DEX_MORPEKO, 0, 20},
                     {DEX_DURALUDON, 0, 10}, {DEX_DREEPY, 0, 10},
                     {DEX_FALINKS, 0, 10}}, 6 } }, 1,
        { { 25, 50, {{DEX_IMPIDIMP, 0, 40}, {DEX_HATENNA, 0, 35}, {DEX_DREEPY, 0, 25}}, 3 } }, 1,
    },
};

const int GALAR_ENCOUNTER_TABLE_COUNT =
    (int)(sizeof(GALAR_ENCOUNTER_TABLES) / sizeof(GALAR_ENCOUNTER_TABLES[0]));

// Lookup by area_id. Returns nullptr if not found.
const EncounterArea* galar_find_area(int area_id) {
    for (int i = 0; i < GALAR_ENCOUNTER_TABLE_COUNT; ++i) {
        if (GALAR_ENCOUNTER_TABLES[i].area_id == (uint16_t)area_id)
            return &GALAR_ENCOUNTER_TABLES[i];
    }
    return nullptr;
}
