# Retail berry farming tables

`item-berry.json` / `item-seed.json` from the open-source PokeMMO Hub
(https://github.com/PokeMMO-Tools/pokemmo-hub, src/data/pokemmo/), fetched 2026-09-19. They are a
dump of the client's own berry data (data.pak section 7 carries the same fields; verify before
relying on the copy). Berry ids 600+k are Gen 5 item 149+k (Cheri .. Rowap, 64 entries).
Seeds 701-705 = Plain (degree 1) per flavor spicy/dry/sweet/bitter/sour, 706-710 = Very
(degree 2); our item ids are 1030-1039 in the same order.

Rules (PokeMMO wiki "Berries" + the Berry Farming Guide): a plant takes 1-3 seeds whose flavor
degrees sum to the berry's degrees; grow_time in real hours; thirst shown as water droplets,
first_water_time hours until the first watering, other_water_time between waterings; dry (0
droplets, flashing red) lowers max yield, watering at 5 droplets floods (yield down), watering a
flooded plant kills it, staying dry too long kills it, not harvesting within wither_time (8h) of
ripening kills it. Plots: Hoenn Route 104 (11), Route 120 (10), Route 123 (12) - the ROM's own
soil spots; Unova Mistralton City (72) and Abundant Shrine (84) - retail's placement. Wailmer
Pail waters in Hoenn (Route 104 flower shop), Sprayduck in Unova (man at Mistralton's plots).
