.class public final synthetic Lf/g10;
.super Ljava/lang/Object;
.source "r8-map-id-21a863b15956229bafbacbd9400e9628b6470120c5fc77a80c1de31b2e861762"

# interfaces
.implements Ljava/lang/Runnable;


# instance fields
.field public final synthetic BU0:F

.field public final synthetic ML0:S

.field public final synthetic Po:F

.field public final synthetic ZS0:B

.field public final synthetic o80:Z


# direct methods
.method public synthetic constructor <init>(SBFFZ)V
    .registers 6

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 2
    .line 3
    .line 4
    iput-short p1, p0, Lf/g10;->ML0:S

    .line 5
    .line 6
    iput-byte p2, p0, Lf/g10;->ZS0:B

    .line 7
    .line 8
    iput p3, p0, Lf/g10;->BU0:F

    .line 9
    .line 10
    iput p4, p0, Lf/g10;->Po:F

    .line 11
    .line 12
    iput-boolean p5, p0, Lf/g10;->o80:Z

    .line 13
    .line 14
    return-void
.end method


# virtual methods
.method public final run()V
    .registers 12

    .line 1
    sget-object v0, Lf/fu0;->br1:Lf/fu0;

    .line 2
    .line 3
    invoke-virtual {v0}, Lf/fu0;->Fi1()F

    .line 4
    .line 5
    .line 6
    move-result v1

    .line 7
    sget v2, Lf/aw3;->YG:F

    .line 8
    .line 9
    cmpg-float v1, v1, v2

    .line 10
    .line 11
    if-gtz v1, :cond_e

    .line 12
    .line 13
    goto/16 :goto_fc

    .line 14
    .line 15
    :cond_e
    invoke-virtual {v0}, Lf/fu0;->Fi1()F

    .line 16
    .line 17
    .line 18
    const/16 v0, 0x1ec

    .line 19
    .line 20
    iget-short v1, p0, Lf/g10;->ML0:S

    .line 21
    .line 22
    iget v7, p0, Lf/g10;->Po:F

    .line 23
    .line 24
    const/high16 v9, 0x3f800000    # 1.0f

    .line 25
    .line 26
    const/4 v4, 0x1

    .line 27
    if-eq v1, v0, :cond_ce

    .line 28
    .line 29
    const/16 v0, 0x41b

    .line 30
    .line 31
    if-eq v1, v0, :cond_b7

    .line 32
    .line 33
    packed-switch v1, :pswitch_data_fe

    .line 34
    .line 35
    .line 36
    packed-switch v1, :pswitch_data_108

    .line 37
    .line 38
    .line 39
    packed-switch v1, :pswitch_data_11a

    .line 40
    .line 41
    .line 42
    goto/16 :goto_d7

    .line 43
    .line 44
    :pswitch_2b
    sget-object v2, Lf/p37;->X20:Lf/t72;

    .line 45
    .line 46
    const v8, 0x3f333333    # 0.7f

    .line 47
    .line 48
    .line 49
    const/4 v10, 0x0

    .line 50
    const/4 v3, 0x2

    .line 51
    const/16 v4, 0x748

    .line 52
    .line 53
    const/4 v5, -0x1

    .line 54
    const/4 v6, 0x1

    .line 55
    invoke-virtual/range {v2 .. v10}, Lf/aw3;->Gz(BSSZFFFI)V

    .line 56
    .line 57
    .line 58
    return-void

    .line 59
    :pswitch_3a
    sget-object v2, Lf/p37;->X20:Lf/t72;

    .line 60
    .line 61
    const v8, 0x3f19999a    # 0.6f

    .line 62
    .line 63
    .line 64
    const/4 v10, 0x0

    .line 65
    const/4 v3, 0x2

    .line 66
    const/4 v4, 0x1

    .line 67
    const/16 v5, 0xe7

    .line 68
    .line 69
    const/4 v6, 0x1

    .line 70
    invoke-virtual/range {v2 .. v10}, Lf/aw3;->Gz(BSSZFFFI)V

    .line 71
    .line 72
    .line 73
    return-void

    .line 74
    :pswitch_49
    sget-object v2, Lf/p37;->X20:Lf/t72;

    .line 75
    .line 76
    const v8, 0x3f19999a    # 0.6f

    .line 77
    .line 78
    .line 79
    const/4 v10, 0x0

    .line 80
    const/4 v3, 0x2

    .line 81
    const/4 v4, 0x1

    .line 82
    const/16 v5, 0x176

    .line 83
    .line 84
    const/4 v6, 0x1

    .line 85
    invoke-virtual/range {v2 .. v10}, Lf/aw3;->Gz(BSSZFFFI)V

    .line 86
    .line 87
    .line 88
    return-void

    .line 89
    :pswitch_58
    const/16 v1, 0x97

    .line 90
    .line 91
    const/16 v5, 0x97

    .line 92
    .line 93
    goto/16 :goto_d8

    .line 94
    .line 95
    :pswitch_5e
    sget-object v3, Lf/p37;->X20:Lf/t72;

    .line 96
    .line 97
    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 98
    .line 99
    .line 100
    sget-object v0, Lf/dq7;->Et:Lf/nx3;

    .line 101
    .line 102
    new-instance v2, Lf/qn6;

    .line 103
    .line 104
    const/4 v5, 0x2

    .line 105
    const/16 v6, 0x7ff

    .line 106
    .line 107
    invoke-direct/range {v2 .. v7}, Lf/qn6;-><init>(Lf/aw3;ZBSF)V

    .line 108
    .line 109
    .line 110
    invoke-virtual {v0, v2}, Lf/nx3;->d3(Ljava/lang/Runnable;)V

    .line 111
    .line 112
    .line 113
    return-void

    .line 114
    :pswitch_71
    const/16 v1, 0x1e7

    .line 115
    .line 116
    const/16 v5, 0x1e7

    .line 117
    .line 118
    goto :goto_d8

    .line 119
    :pswitch_76
    const/16 v1, 0x26e

    .line 120
    .line 121
    const/16 v5, 0x26e

    .line 122
    .line 123
    goto :goto_d8

    .line 124
    :pswitch_7b
    sget-object v2, Lf/p37;->X20:Lf/t72;

    .line 125
    .line 126
    const v8, 0x3f19999a    # 0.6f

    .line 127
    .line 128
    .line 129
    const/4 v10, 0x0

    .line 130
    const/4 v3, 0x2

    .line 131
    const/4 v4, 0x1

    .line 132
    const/16 v5, 0x1c8

    .line 133
    .line 134
    const/4 v6, 0x1

    .line 135
    invoke-virtual/range {v2 .. v10}, Lf/aw3;->Gz(BSSZFFFI)V

    .line 136
    .line 137
    .line 138
    return-void

    .line 139
    :pswitch_8a
    sget-object v2, Lf/p37;->X20:Lf/t72;

    .line 140
    .line 141
    const v8, 0x3f19999a    # 0.6f

    .line 142
    .line 143
    .line 144
    const/4 v10, 0x0

    .line 145
    const/4 v3, 0x2

    .line 146
    const/16 v4, 0x70a

    .line 147
    .line 148
    const/4 v5, -0x1

    .line 149
    const/4 v6, 0x1

    .line 150
    invoke-virtual/range {v2 .. v10}, Lf/aw3;->Gz(BSSZFFFI)V

    .line 151
    .line 152
    .line 153
    return-void

    .line 154
    :pswitch_99
    sget-object v2, Lf/p37;->X20:Lf/t72;

    .line 155
    .line 156
    const v8, 0x3f19999a    # 0.6f

    .line 157
    .line 158
    .line 159
    const/4 v10, 0x0

    .line 160
    const/4 v3, 0x2

    .line 161
    const/16 v4, 0x715

    .line 162
    .line 163
    const/4 v5, -0x1

    .line 164
    const/4 v6, 0x1

    .line 165
    invoke-virtual/range {v2 .. v10}, Lf/aw3;->Gz(BSSZFFFI)V

    .line 166
    .line 167
    .line 168
    return-void

    .line 169
    :pswitch_a8
    const/16 v1, 0x286

    .line 170
    .line 171
    const/16 v5, 0x286

    .line 172
    .line 173
    goto :goto_d8

    .line 174
    :pswitch_ad
    const/16 v1, 0x5d

    .line 175
    .line 176
    const/16 v5, 0x5d

    .line 177
    .line 178
    goto :goto_d8

    .line 179
    :pswitch_b2
    const/16 v1, 0x165

    .line 180
    .line 181
    const/16 v5, 0x165

    .line 182
    .line 183
    goto :goto_d8

    .line 184
    :cond_b7
    sget-object v2, Lf/p37;->X20:Lf/t72;

    .line 185
    .line 186
    const/high16 v8, 0x3f800000    # 1.0f

    .line 187
    .line 188
    const/4 v10, 0x0

    .line 189
    const/4 v3, 0x2

    .line 190
    const/16 v4, 0x5d1

    .line 191
    .line 192
    const/4 v5, -0x1

    .line 193
    const/4 v6, 0x1

    .line 194
    invoke-virtual/range {v2 .. v10}, Lf/aw3;->Gz(BSSZFFFI)V

    .line 195
    .line 196
    .line 197
    sget-object v2, Lf/p37;->X20:Lf/t72;

    .line 198
    .line 199
    const/16 v10, 0x190

    .line 200
    .line 201
    const/16 v4, 0x59c

    .line 202
    .line 203
    invoke-virtual/range {v2 .. v10}, Lf/aw3;->Gz(BSSZFFFI)V

    .line 204
    .line 205
    .line 206
    return-void

    .line 207
    :cond_ce
    iget-byte v0, p0, Lf/g10;->ZS0:B

    .line 208
    .line 209
    if-ne v0, v4, :cond_d7

    .line 210
    .line 211
    const/16 v1, 0x28a

    .line 212
    .line 213
    const/16 v5, 0x28a

    .line 214
    .line 215
    goto :goto_d8

    .line 216
    :cond_d7
    :goto_d7
    # MonMMO-EX: a cry the resources tree supplied as a file (data/resources/cries/<id>.wav, registered
    # by c85.ZY1 under the bundled-sound region 10) plays through the same file player as the client's
    # own sounds/10 oggs. Retail only knows ROM cries - loadSSEQ(2, 1, species) on the Black ROM - so an
    # Expansion species had no cry at all on this client. Anything without a file still takes the ROM path.
    #
    # RETAIL SPECIES NEVER CONSULT THE MOD MAP. Region 10 is shared with the client's OWN bundled
    # sounds (ids 1..22, the mount engine loop among them), so asking it for species 6 handed
    # Charizard the motorcycle the owner rides (2026-09-21). Only ids at or above the first
    # Expansion species (650 - the same 0x28a the retail code above uses as its no-ROM-cry marker)
    # may have a mod file, and every cry we ship is 668 or higher.
    const/16 v0, 0x28a
    if-lt v1, v0, :monmmo_rom_cry
    sget-object v0, Lf/p37;->X20:Lf/t72;
    iget-object v0, v0, Lf/aw3;->SE0:Lf/k89;
    const/high16 v5, 0xa0000
    add-int/2addr v5, v1
    invoke-virtual {v0, v5}, Lf/x44;->US1(I)Z
    move-result v0
    if-eqz v0, :monmmo_rom_cry
    sget-object v2, Lf/p37;->X20:Lf/t72;
    const/16 v3, 0xa
    move v4, v1
    const/4 v5, -0x1
    const/4 v6, 0x1
    iget v8, p0, Lf/g10;->BU0:F
    const/4 v10, 0x0
    invoke-virtual/range {v2 .. v10}, Lf/aw3;->Gz(BSSZFFFI)V
    return-void
    :monmmo_rom_cry
    move v5, v1

    .line 217
    :goto_d8
    sget-object v0, Lf/dq7;->Et:Lf/nx3;

    .line 218
    .line 219
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 220
    .line 221
    .line 222
    sget-object v0, Lf/le3;->oj0:Lf/le3;

    .line 223
    .line 224
    sget-object v2, Lf/p37;->X20:Lf/t72;

    .line 225
    .line 226
    const/4 v6, 0x1

    .line 227
    const/4 v10, 0x0

    .line 228
    const/4 v3, 0x2

    .line 229
    const/4 v4, 0x1

    .line 230
    iget v8, p0, Lf/g10;->BU0:F

    .line 231
    .line 232
    invoke-virtual/range {v2 .. v10}, Lf/aw3;->Gz(BSSZFFFI)V

    .line 233
    .line 234
    .line 235
    iget-boolean v0, p0, Lf/g10;->o80:Z

    .line 236
    .line 237
    if-eqz v0, :cond_fc

    .line 238
    .line 239
    sget-object v2, Lf/p37;->X20:Lf/t72;

    .line 240
    .line 241
    const/high16 v0, 0x3f000000    # 0.5f

    .line 242
    .line 243
    mul-float v8, v8, v0

    .line 244
    .line 245
    const/high16 v9, 0x3f000000    # 0.5f

    .line 246
    .line 247
    const/4 v10, 0x0

    .line 248
    const/4 v3, 0x2

    .line 249
    const/4 v6, 0x1

    .line 250
    invoke-virtual/range {v2 .. v10}, Lf/aw3;->Gz(BSSZFFFI)V

    .line 251
    .line 252
    .line 253
    :cond_fc
    :goto_fc
    return-void

    .line 254
    nop

    .line 255
    :pswitch_data_fe
    .packed-switch 0x3e8
        :pswitch_b2
        :pswitch_ad
        :pswitch_a8
    .end packed-switch

    .line 256
    .line 257
    .line 258
    .line 259
    .line 260
    .line 261
    .line 262
    .line 263
    .line 264
    .line 265
    :pswitch_data_108
    .packed-switch 0x3fb
        :pswitch_99
        :pswitch_8a
        :pswitch_7b
        :pswitch_76
        :pswitch_71
        :pswitch_5e
        :pswitch_58
    .end packed-switch

    .line 266
    .line 267
    .line 268
    .line 269
    .line 270
    .line 271
    .line 272
    .line 273
    .line 274
    .line 275
    .line 276
    .line 277
    .line 278
    .line 279
    .line 280
    .line 281
    .line 282
    .line 283
    :pswitch_data_11a
    .packed-switch 0x417
        :pswitch_49
        :pswitch_3a
        :pswitch_2b
    .end packed-switch
.end method
