#pragma once
#include <stdint.h>

// Galar encounter table following pkNX EncounterTable8 structure.
// Symbol = visible overworld spawns. Hidden = grass/random encounter.
// Probability values sum to 100 per subtable.
// Area IDs map to Galar routes/zones; used by SwordBridge pick_encounter().
// Source: reverse-engineered from pkNX SSWE editor and known Sword v1.3.2 data.

typedef struct {
    uint16_t species;
    uint8_t  form;       // 0 = base form
    uint8_t  probability; // percent, 0-100
} EncSlot;

typedef struct {
    uint8_t  level_min;
    uint8_t  level_max;
    EncSlot  slots[12];
    uint8_t  slot_count;
} SubTable;

typedef struct {
    uint16_t   area_id;
    const char* name;
    SubTable   symbol[4]; // overworld visible
    uint8_t    symbol_count;
    SubTable   hidden[4]; // grass / tall grass
    uint8_t    hidden_count;
} EncounterArea;

// ── Area ID constants ────────────────────────────────────────────────────────
#define GALAR_ROUTE_1          1
#define GALAR_ROUTE_2          2
#define GALAR_ROUTE_3          3
#define GALAR_MINE_1           4
#define GALAR_ROUTE_4          5
#define GALAR_ROUTE_5          6
#define GALAR_MINE_2           7
#define GALAR_ROUTE_6          8
#define GALAR_ROUTE_7          9
#define GALAR_ROUTE_8          10
#define GALAR_ROUTE_9          11
#define GALAR_ROUTE_10         12
#define GALAR_WILD_SOUTH       20
#define GALAR_WILD_EAST        21
#define GALAR_WILD_NORTH       22
#define GALAR_WILD_LAKE        23
#define GALAR_WILD_GIANT_SEAT  24
#define GALAR_WILD_HAMMERLOCK  25
#define GALAR_WILD_DUSTY_BOWL  26
#define GALAR_WILD_GIANT_CAP   27
#define GALAR_WILD_SNOWFIELDS  28
#define GALAR_WILD_WATCHTOWER  29
#define GALAR_WILD_BRIDGE      30

// Dex IDs (Gen 8 National Dex)
#define DEX_SKWOVET    819
#define DEX_ROOKIDEE   821
#define DEX_NICKIT     827
#define DEX_GOSSIFLEUR 829
#define DEX_WOOLOO     831
#define DEX_CHEWTLE    833
#define DEX_YAMPER     835
#define DEX_ROLYCOLY   837
#define DEX_APPLIN     840
#define DEX_SILICOBRA  843
#define DEX_CRAMORANT  845
#define DEX_TOXEL      848
#define DEX_SIZZLIPEDE 850
#define DEX_CLOBBOPUS  852
#define DEX_SINISTEA   854
#define DEX_HATENNA    856
#define DEX_IMPIDIMP   859
#define DEX_FALINKS    870
#define DEX_PINCURCHIN 871
#define DEX_SNOM       872
#define DEX_EISCUE     875
#define DEX_MORPEKO    877
#define DEX_DREEPY     885
#define DEX_DURALUDON  884
// Cross-gen in Galar dex
#define DEX_MEOWTH     52
#define DEX_PONYTA     77
#define DEX_FARFETCHD  83
#define DEX_SLOWPOKE   79
#define DEX_MR_MIME    122
#define DEX_ZIGZAGOON  263
#define DEX_STUNFISK   618
#define DEX_CORSOLA    222
#define DEX_DARUMAKA   554
#define DEX_EEVEE      133
#define DEX_WOOBAT     527

extern const EncounterArea GALAR_ENCOUNTER_TABLES[];
extern const int GALAR_ENCOUNTER_TABLE_COUNT;
