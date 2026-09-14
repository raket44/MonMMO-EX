// MonMMO-EX's own species, read by ExpansionSpeciesGenerator after the Expansion's species_info.
// Written in the Expansion's syntax but NOT preprocessed: plain numbers, no config ternaries, no
// macros. Each entry is numbered after the Expansion's highest species id, in file order, so an
// entry appended here never moves an existing wire id. Art paths are relative to this directory.
// Beside the decomp-style art a species folder may carry monmmo_front.gif / monmmo_back.gif
// (animated battle sprites, used as-is) and monmmo_icon.pal (the icon's own palette).

// Crystal Onix: the Cerulean Cave raid boss, a form of Onix (project owner, 2026-09-14). Art by
// the owner; no shiny art exists, so the shiny palettes repeat the normal ones.
static const u16 sOnixFormSpeciesIdTable[] = {
    SPECIES_ONIX,
    SPECIES_ONIX_CRYSTAL,
    FORM_SPECIES_END,
};

const u32 gMonFrontPic_OnixCrystal[] = INCBIN_U32("onix_crystal/anim_front.png");
const u32 gMonBackPic_OnixCrystal[] = INCBIN_U32("onix_crystal/back.png");
const u16 gMonPalette_OnixCrystal[] = INCBIN_U16("onix_crystal/normal.pal");
const u16 gMonShinyPalette_OnixCrystal[] = INCBIN_U16("onix_crystal/shiny.pal");
const u8 gMonIcon_OnixCrystal[] = INCBIN_U8("onix_crystal/icon.png");

    [SPECIES_ONIX_CRYSTAL] =
    {
        .baseHP        = 150,
        .baseAttack    = 95,
        .baseDefense   = 180,
        .baseSpeed     = 60,
        .baseSpAttack  = 95,
        .baseSpDefense = 150,
        .types = { TYPE_ROCK, TYPE_FAIRY },
        .catchRate = 3,
        .expYield = 270,
        .evYield_Defense = 2,
        .evYield_SpDefense = 1,
        .genderRatio = MON_GENDERLESS,
        .eggCycles = 120,
        .friendship = 0,
        .growthRate = GROWTH_SLOW,
        .eggGroups = { EGG_GROUP_NO_EGGS_DISCOVERED },
        .abilities = { ABILITY_STURDY, ABILITY_STURDY, ABILITY_STURDY },
        .bodyColor = BODY_COLOR_BLUE,
        .speciesName = _("Crystal Onix"),
        .cryId = CRY_ONIX,
        .natDexNum = NATIONAL_DEX_ONIX,
        .categoryName = _("Crystal Snake"),
        .height = 88,
        .weight = 2600,
        .description = COMPOUND_STRING(
            "Deep in Cerulean Cave, an Onix grew\n"
            "a hide of living crystal. Its shards\n"
            "rain down on intruders, and wild Onix\n"
            "answer when it calls."),
        .frontPic = gMonFrontPic_OnixCrystal,
        .backPic = gMonBackPic_OnixCrystal,
        .palette = gMonPalette_OnixCrystal,
        .shinyPalette = gMonShinyPalette_OnixCrystal,
        .iconSprite = gMonIcon_OnixCrystal,
        .overworldData = { gMonIcon_OnixCrystal },
        .levelUpLearnset = sOnixLevelUpLearnset,
        .formSpeciesIdTable = sOnixFormSpeciesIdTable,
    },
