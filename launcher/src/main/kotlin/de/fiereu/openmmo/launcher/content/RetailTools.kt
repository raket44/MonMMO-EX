package de.fiereu.openmmo.launcher.content

/**
 * The client's own tool items, for the numberless rename (operator-directed: names read as "TM
 * <move>" everywhere - pickups, dialogs, bag, dex - never a bare number).
 *
 * PokeMMO's TM catalogue is the Gen 5 set: TM01-TM95 as items 5328-5422 (itemId = 5327 + number),
 * with the 6xxx twins sharing the same name string. Item names resolve from string id `240000 +
 * itemId` (f/Gc0.fb - measured on the live client: TM24 item 5351 carries fb 245351 and teaches
 * move 85 Thunderbolt, TM29 item 5356 fb 245356 teaches 94 Psychic; both anchor the Gen 5
 * numbering). A strings_en.xml entry at that id overrides the name everywhere it renders. HMs are
 * the ROM items 339-346 (the GBA HM01-08 set).
 *
 * The DexPatch "dumptoolsfile" diagnostic writes every live tool to monmmo-tools.csv on boot, so
 * any drift between this table and the client is caught on the next launch rather than trusted.
 */
object RetailTools {
  const val TM_ITEM_BASE = 5327
  const val ITEM_NAME_STRING_BASE = 240000

  /** Gen 5 TM number to canonical move id. */
  val TM_MOVES =
      listOf(
          468, // TM01 Hone Claws
          337, // TM02 Dragon Claw
          473, // TM03 Psyshock
          347, // TM04 Calm Mind
          46, // TM05 Roar
          92, // TM06 Toxic
          258, // TM07 Hail
          339, // TM08 Bulk Up
          474, // TM09 Venoshock
          237, // TM10 Hidden Power
          241, // TM11 Sunny Day
          269, // TM12 Taunt
          58, // TM13 Ice Beam
          59, // TM14 Blizzard
          63, // TM15 Hyper Beam
          113, // TM16 Light Screen
          182, // TM17 Protect
          240, // TM18 Rain Dance
          477, // TM19 Telekinesis
          219, // TM20 Safeguard
          218, // TM21 Frustration
          76, // TM22 Solar Beam
          479, // TM23 Smack Down
          85, // TM24 Thunderbolt (anchor: live item 5351, fb 245351)
          87, // TM25 Thunder
          89, // TM26 Earthquake
          216, // TM27 Return
          91, // TM28 Dig
          94, // TM29 Psychic (anchor: live item 5356, fb 245356)
          247, // TM30 Shadow Ball
          280, // TM31 Brick Break
          104, // TM32 Double Team
          115, // TM33 Reflect
          482, // TM34 Sludge Wave
          53, // TM35 Flamethrower
          188, // TM36 Sludge Bomb
          201, // TM37 Sandstorm
          126, // TM38 Fire Blast
          317, // TM39 Rock Tomb
          332, // TM40 Aerial Ace
          259, // TM41 Torment
          263, // TM42 Facade
          488, // TM43 Flame Charge
          156, // TM44 Rest
          213, // TM45 Attract
          168, // TM46 Thief
          490, // TM47 Low Sweep
          496, // TM48 Round
          497, // TM49 Echoed Voice
          315, // TM50 Overheat
          502, // TM51 Ally Switch
          411, // TM52 Focus Blast
          412, // TM53 Energy Ball
          206, // TM54 False Swipe
          503, // TM55 Scald
          374, // TM56 Fling
          451, // TM57 Charge Beam
          507, // TM58 Sky Drop
          510, // TM59 Incinerate
          511, // TM60 Quash
          261, // TM61 Will-O-Wisp
          512, // TM62 Acrobatics
          373, // TM63 Embargo
          153, // TM64 Explosion
          421, // TM65 Shadow Claw
          371, // TM66 Payback
          514, // TM67 Retaliate
          416, // TM68 Giga Impact
          397, // TM69 Rock Polish
          148, // TM70 Flash
          444, // TM71 Stone Edge
          521, // TM72 Volt Switch
          86, // TM73 Thunder Wave
          360, // TM74 Gyro Ball
          14, // TM75 Swords Dance
          522, // TM76 Struggle Bug
          244, // TM77 Psych Up
          523, // TM78 Bulldoze
          524, // TM79 Frost Breath
          157, // TM80 Rock Slide
          404, // TM81 X-Scissor
          525, // TM82 Dragon Tail
          526, // TM83 Work Up
          398, // TM84 Poison Jab
          138, // TM85 Dream Eater
          447, // TM86 Grass Knot
          207, // TM87 Swagger
          365, // TM88 Pluck
          369, // TM89 U-turn
          164, // TM90 Substitute
          430, // TM91 Flash Cannon
          433, // TM92 Trick Room
      )

  /**
   * TM93-95 sit at items 5618-5620, NOT at 5327+n - the ids after TM92 hold the Unova HM set.
   * Measured off monmmo-tools.csv: 5618 teaches 528 Wild Charge, 5619 -> 249 Rock Smash, 5620 ->
   * 555 Snarl; naming them by the 5327+n formula stamped TM names onto HM items.
   */
  val HIGH_TM_ITEMS =
      listOf(
          5618 to 528, // TM93 Wild Charge
          5619 to 249, // TM94 Rock Smash
          5620 to 555, // TM95 Snarl
      )

  /**
   * Every regional HM set, to canonical move id - all measured off monmmo-tools.csv. The GBA set is
   * the ROM items 339-346; Unova, Sinnoh and Johto carry their own HM item blocks (5420+, 8420+,
   * 9420+), which the old TM formula partly mislabeled as TM93-95.
   */
  val HM_ITEMS =
      listOf(
          339 to 15, // GBA HM01 Cut
          340 to 19, // GBA HM02 Fly
          341 to 57, // GBA HM03 Surf
          342 to 70, // GBA HM04 Strength
          343 to 148, // GBA HM05 Flash
          344 to 249, // GBA HM06 Rock Smash
          345 to 127, // GBA HM07 Waterfall
          346 to 291, // GBA HM08 Dive
          5420 to 15, // Unova HM01 Cut
          5421 to 19, // Unova HM02 Fly
          5422 to 57, // Unova HM03 Surf
          5423 to 70, // Unova HM04 Strength
          5424 to 127, // Unova HM05 Waterfall
          5425 to 291, // Unova HM06 Dive
          8420 to 15, // Sinnoh HM01 Cut
          8421 to 19, // Sinnoh HM02 Fly
          8422 to 57, // Sinnoh HM03 Surf
          8423 to 70, // Sinnoh HM04 Strength
          8424 to 432, // Sinnoh HM05 Defog
          8425 to 249, // Sinnoh HM06 Rock Smash
          8426 to 127, // Sinnoh HM07 Waterfall
          8427 to 431, // Sinnoh HM08 Rock Climb
          9420 to 15, // Johto HM01 Cut
          9421 to 19, // Johto HM02 Fly
          9422 to 57, // Johto HM03 Surf
          9423 to 70, // Johto HM04 Strength
          9424 to 250, // Johto HM05 Whirlpool
          9425 to 249, // Johto HM06 Rock Smash
          9426 to 127, // Johto HM07 Waterfall
          9427 to 431, // Johto HM08 Rock Climb
      )

  /** (name string id, move id) for every retail TM to rename. */
  fun renames(): List<Pair<Int, Int>> =
      TM_MOVES.mapIndexed { index, moveId ->
        (ITEM_NAME_STRING_BASE + TM_ITEM_BASE + index + 1) to moveId
      } + HIGH_TM_ITEMS.map { (itemId, moveId) -> (ITEM_NAME_STRING_BASE + itemId) to moveId }

  /** (name string id, move id) for every retail HM, named "HM <move>". */
  fun hmRenames(): List<Pair<Int, Int>> =
      HM_ITEMS.map { (itemId, moveId) -> (ITEM_NAME_STRING_BASE + itemId) to moveId }
}
