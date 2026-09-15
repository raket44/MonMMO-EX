.class public final Lf/zw0;
.super Ljava/lang/Object;


# static fields
.field public static final XW:Lf/xv7;


# instance fields
.field public CU:[Lf/sg7;

.field public JO0:Lf/th7;

.field public WP1:Lf/rf3;

.field public aR1:[Lf/th7;

.field public i9:[B

.field public iT0:Lf/th7;

.field public lW0:Lf/l91;

.field public pH1:Lf/v67;

.field public yf:Z


# direct methods
.method static constructor <clinit>()V
    .registers 1

    .line 1
    const-class v0, Lf/zw0;

    .line 2
    .line 3
    invoke-static {v0}, Lf/tv7;->I80(Ljava/lang/Class;)Lf/xv7;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    sput-object v0, Lf/zw0;->XW:Lf/xv7;

    .line 8
    .line 9
    return-void
.end method

.method public static ls(Ljava/lang/Exception;)Z
    .registers 3

    .line 1
    :goto_0
    if-eqz p0, :cond_1e

    .line 2
    .line 3
    const-class v0, Ljava/lang/OutOfMemoryError;

    .line 4
    .line 5
    invoke-virtual {v0, p0}, Ljava/lang/Class;->isInstance(Ljava/lang/Object;)Z

    .line 6
    .line 7
    .line 8
    move-result v0

    .line 9
    if-eqz v0, :cond_b

    .line 10
    .line 11
    goto :goto_17

    .line 12
    :cond_b
    const-string v0, "Map failed"

    .line 13
    .line 14
    invoke-virtual {p0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    .line 15
    .line 16
    .line 17
    move-result-object v1

    .line 18
    invoke-virtual {v0, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    .line 19
    .line 20
    .line 21
    move-result v0

    .line 22
    if-eqz v0, :cond_19

    .line 23
    .line 24
    :goto_17
    const/4 p0, 0x1

    .line 25
    return p0

    .line 26
    :cond_19
    invoke-virtual {p0}, Ljava/lang/Throwable;->getCause()Ljava/lang/Throwable;

    .line 27
    .line 28
    .line 29
    move-result-object p0

    .line 30
    goto :goto_0

    .line 31
    :cond_1e
    const/4 p0, 0x0

    .line 32
    return p0
.end method


# virtual methods
.method public final Oe(B)Z
    .registers 5

    # MonMMO-EX: Pokedex regions 6-9 (Kalos, Alola, Galar, Paldea) have no ROM to check; always available.
    const/4 v2, 0x6
    if-lt p1, v2, :monmmo_stock_regions
    const/16 v2, 0xa
    if-ge p1, v2, :monmmo_stock_regions
    const/4 v2, 0x1
    return v2

    :monmmo_stock_regions

    .line 1
    const/4 v0, 0x0

    .line 2
    const/4 v1, 0x1

    .line 3
    if-eqz p1, :cond_31

    .line 4
    .line 5
    if-eq p1, v1, :cond_2b

    .line 6
    .line 7
    const/4 v2, 0x2

    .line 8
    if-eq p1, v2, :cond_25

    .line 9
    .line 10
    const/4 v2, 0x3

    .line 11
    if-eq p1, v2, :cond_1f

    .line 12
    .line 13
    const/4 v2, 0x4

    .line 14
    if-eq p1, v2, :cond_19

    .line 15
    .line 16
    const/4 v2, 0x5

    .line 17
    if-eq p1, v2, :cond_18

    .line 18
    .line 19
    const/16 v2, 0xa

    .line 20
    .line 21
    if-eq p1, v2, :cond_17

    .line 22
    .line 23
    return v0

    .line 24
    :cond_17
    return v1

    .line 25
    :cond_18
    return v0

    .line 26
    :cond_19
    iget-object p1, p0, Lf/zw0;->WP1:Lf/rf3;

    .line 27
    .line 28
    if-eqz p1, :cond_1e

    .line 29
    .line 30
    return v1

    .line 31
    :cond_1e
    return v0

    .line 32
    :cond_1f
    iget-object p1, p0, Lf/zw0;->lW0:Lf/l91;

    .line 33
    .line 34
    if-eqz p1, :cond_24

    .line 35
    .line 36
    return v1

    .line 37
    :cond_24
    return v0

    .line 38
    :cond_25
    iget-object p1, p0, Lf/zw0;->pH1:Lf/v67;

    .line 39
    .line 40
    if-eqz p1, :cond_2a

    .line 41
    .line 42
    return v1

    .line 43
    :cond_2a
    return v0

    .line 44
    :cond_2b
    iget-object p1, p0, Lf/zw0;->iT0:Lf/th7;

    .line 45
    .line 46
    if-eqz p1, :cond_30

    .line 47
    .line 48
    return v1

    .line 49
    :cond_30
    return v0

    .line 50
    :cond_31
    iget-object p1, p0, Lf/zw0;->JO0:Lf/th7;

    .line 51
    .line 52
    if-eqz p1, :cond_36

    .line 53
    .line 54
    return v1

    .line 55
    :cond_36
    return v0
.end method

.method public final Ux(B)Lf/sg7;
    .registers 3

    .line 1
    iget-object v0, p0, Lf/zw0;->pH1:Lf/v67;

    .line 2
    .line 3
    if-eqz v0, :cond_d

    .line 4
    .line 5
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 6
    .line 7
    .line 8
    const/4 v0, 0x2

    .line 9
    if-ne v0, p1, :cond_d

    .line 10
    .line 11
    iget-object p1, p0, Lf/zw0;->pH1:Lf/v67;

    .line 12
    .line 13
    return-object p1

    .line 14
    :cond_d
    iget-object v0, p0, Lf/zw0;->lW0:Lf/l91;

    .line 15
    .line 16
    if-eqz v0, :cond_1a

    .line 17
    .line 18
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 19
    .line 20
    .line 21
    const/4 v0, 0x3

    .line 22
    if-ne v0, p1, :cond_1a

    .line 23
    .line 24
    iget-object p1, p0, Lf/zw0;->lW0:Lf/l91;

    .line 25
    .line 26
    return-object p1

    .line 27
    :cond_1a
    iget-object v0, p0, Lf/zw0;->WP1:Lf/rf3;

    .line 28
    .line 29
    if-eqz v0, :cond_27

    .line 30
    .line 31
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 32
    .line 33
    .line 34
    const/4 v0, 0x4

    .line 35
    if-ne v0, p1, :cond_27

    .line 36
    .line 37
    iget-object p1, p0, Lf/zw0;->WP1:Lf/rf3;

    .line 38
    .line 39
    return-object p1

    .line 40
    :cond_27
    const/4 p1, 0x0

    .line 41
    return-object p1
.end method

.method public final c91(B)Lf/th7;
    .registers 3

    .line 1
    iget-object v0, p0, Lf/zw0;->JO0:Lf/th7;

    .line 2
    .line 3
    if-eqz v0, :cond_d

    .line 4
    .line 5
    invoke-virtual {v0}, Lf/th7;->sB1()B

    .line 6
    .line 7
    .line 8
    move-result v0

    .line 9
    if-ne v0, p1, :cond_d

    .line 10
    .line 11
    iget-object p1, p0, Lf/zw0;->JO0:Lf/th7;

    .line 12
    .line 13
    return-object p1

    .line 14
    :cond_d
    iget-object v0, p0, Lf/zw0;->iT0:Lf/th7;

    .line 15
    .line 16
    if-eqz v0, :cond_1a

    .line 17
    .line 18
    invoke-virtual {v0}, Lf/th7;->sB1()B

    .line 19
    .line 20
    .line 21
    move-result v0

    .line 22
    if-ne v0, p1, :cond_1a

    .line 23
    .line 24
    iget-object p1, p0, Lf/zw0;->iT0:Lf/th7;

    .line 25
    .line 26
    return-object p1

    .line 27
    :cond_1a
    const/4 p1, 0x0

    .line 28
    return-object p1
.end method

.method public final hr1()V
    .registers 16

    .line 1
    sget-object v0, Lf/dq7;->vZ1:Lf/u43;

    .line 2
    .line 3
    const-string v1, "roms"

    .line 4
    .line 5
    invoke-virtual {v0, v1}, Lf/u43;->xo0(Ljava/lang/String;)Lf/rz;

    .line 6
    .line 7
    .line 8
    move-result-object v0

    .line 9
    const-string v1, "Could not load {} as a gba rom because: {}"

    .line 10
    .line 11
    new-instance v2, Lf/rh6;

    .line 12
    .line 13
    invoke-direct {v2}, Lf/rh6;-><init>()V

    .line 14
    .line 15
    .line 16
    sget-object v3, Lf/ms5;->IQ1:Ljava/lang/String;

    .line 17
    .line 18
    sget-object v4, Lf/ms5;->TG:Ljava/lang/String;

    .line 19
    .line 20
    sget-object v5, Lf/ms5;->qi0:Ljava/lang/String;

    .line 21
    .line 22
    sget-object v6, Lf/ms5;->zT1:Ljava/lang/String;

    .line 23
    .line 24
    sget-object v7, Lf/ms5;->EK1:Ljava/lang/String;

    .line 25
    .line 26
    sget-object v8, Lf/ms5;->T01:Ljava/lang/String;

    .line 27
    .line 28
    filled-new-array/range {v3 .. v8}, [Ljava/lang/String;

    .line 29
    .line 30
    .line 31
    move-result-object v3

    .line 32
    const/4 v4, 0x0

    .line 33
    const/4 v5, 0x6

    .line 34
    invoke-virtual {v2, v3, v4, v5}, Lf/rh6;->N80([Ljava/lang/Object;II)V

    .line 35
    .line 36
    .line 37
    const-string v3, "_import"

    .line 38
    .line 39
    invoke-virtual {v0, v3}, Lf/rz;->BL0(Ljava/lang/String;)[Lf/z46;

    .line 40
    .line 41
    .line 42
    move-result-object v3

    .line 43
    invoke-static {v3}, Lj$/util/DesugarArrays;->stream([Ljava/lang/Object;)Lj$/util/stream/Stream;

    .line 44
    .line 45
    .line 46
    move-result-object v3

    .line 47
    new-instance v5, Lf/do5;

    .line 48
    .line 49
    invoke-direct {v5, v4}, Lf/do5;-><init>(I)V

    .line 50
    .line 51
    .line 52
    invoke-interface {v3, v5}, Lj$/util/stream/Stream;->map(Ljava/util/function/Function;)Lj$/util/stream/Stream;

    .line 53
    .line 54
    .line 55
    move-result-object v3

    .line 56
    new-instance v5, Lf/v55;

    .line 57
    .line 58
    const/4 v6, 0x2

    .line 59
    invoke-direct {v5, v2, v6}, Lf/v55;-><init>(Lf/rh6;I)V

    .line 60
    .line 61
    .line 62
    invoke-interface {v3, v5}, Lj$/util/stream/Stream;->forEach(Ljava/util/function/Consumer;)V

    .line 63
    .line 64
    .line 65
    new-instance v2, Ljava/util/ArrayList;

    .line 66
    .line 67
    invoke-direct {v2}, Ljava/util/ArrayList;-><init>()V

    .line 68
    .line 69
    .line 70
    sget-object v3, Lf/ms5;->TG:Ljava/lang/String;

    .line 71
    .line 72
    invoke-virtual {v3}, Ljava/lang/String;->isEmpty()Z

    .line 73
    .line 74
    .line 75
    move-result v3

    .line 76
    if-nez v3, :cond_58

    .line 77
    .line 78
    sget-object v3, Lf/p37;->Uh0:Lf/o44;

    .line 79
    .line 80
    sget-object v5, Lf/ms5;->TG:Ljava/lang/String;

    .line 81
    .line 82
    invoke-virtual {v3, v5}, Lf/o44;->iR1(Ljava/lang/String;)Lf/z46;

    .line 83
    .line 84
    .line 85
    move-result-object v3

    .line 86
    invoke-virtual {v2, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 87
    .line 88
    .line 89
    :cond_58
    sget-object v3, Lf/ms5;->IQ1:Ljava/lang/String;

    .line 90
    .line 91
    invoke-virtual {v3}, Ljava/lang/String;->isEmpty()Z

    .line 92
    .line 93
    .line 94
    move-result v3

    .line 95
    if-nez v3, :cond_6b

    .line 96
    .line 97
    sget-object v3, Lf/p37;->Uh0:Lf/o44;

    .line 98
    .line 99
    sget-object v5, Lf/ms5;->IQ1:Ljava/lang/String;

    .line 100
    .line 101
    invoke-virtual {v3, v5}, Lf/o44;->iR1(Ljava/lang/String;)Lf/z46;

    .line 102
    .line 103
    .line 104
    move-result-object v3

    .line 105
    invoke-virtual {v2, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 106
    .line 107
    .line 108
    :cond_6b
    sget-object v3, Lf/ms5;->qi0:Ljava/lang/String;

    .line 109
    .line 110
    invoke-virtual {v3}, Ljava/lang/String;->isEmpty()Z

    .line 111
    .line 112
    .line 113
    move-result v3

    .line 114
    if-nez v3, :cond_7e

    .line 115
    .line 116
    sget-object v3, Lf/p37;->Uh0:Lf/o44;

    .line 117
    .line 118
    sget-object v5, Lf/ms5;->qi0:Ljava/lang/String;

    .line 119
    .line 120
    invoke-virtual {v3, v5}, Lf/o44;->iR1(Ljava/lang/String;)Lf/z46;

    .line 121
    .line 122
    .line 123
    move-result-object v3

    .line 124
    invoke-virtual {v2, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 125
    .line 126
    .line 127
    :cond_7e
    sget-object v3, Lf/ms5;->zT1:Ljava/lang/String;

    .line 128
    .line 129
    invoke-virtual {v3}, Ljava/lang/String;->isEmpty()Z

    .line 130
    .line 131
    .line 132
    move-result v3

    .line 133
    sget-object v5, Lf/zw0;->XW:Lf/xv7;

    .line 134
    .line 135
    if-nez v3, :cond_9c

    .line 136
    .line 137
    sget-object v3, Lf/p37;->Uh0:Lf/o44;

    .line 138
    .line 139
    sget-object v7, Lf/ms5;->zT1:Ljava/lang/String;

    .line 140
    .line 141
    invoke-virtual {v3, v7}, Lf/o44;->iR1(Ljava/lang/String;)Lf/z46;

    .line 142
    .line 143
    .line 144
    move-result-object v3

    .line 145
    if-nez v3, :cond_99

    .line 146
    .line 147
    const-string v7, "Config BW ROM Location is null: {}"

    .line 148
    .line 149
    sget-object v8, Lf/ms5;->zT1:Ljava/lang/String;

    .line 150
    .line 151
    invoke-interface {v5, v7, v8}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    .line 152
    .line 153
    .line 154
    :cond_99
    invoke-virtual {v2, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 155
    .line 156
    .line 157
    :cond_9c
    sget-object v3, Lf/ms5;->EK1:Ljava/lang/String;

    .line 158
    .line 159
    invoke-virtual {v3}, Ljava/lang/String;->isEmpty()Z

    .line 160
    .line 161
    .line 162
    move-result v3

    .line 163
    if-nez v3, :cond_af

    .line 164
    .line 165
    sget-object v3, Lf/p37;->Uh0:Lf/o44;

    .line 166
    .line 167
    sget-object v7, Lf/ms5;->EK1:Ljava/lang/String;

    .line 168
    .line 169
    invoke-virtual {v3, v7}, Lf/o44;->iR1(Ljava/lang/String;)Lf/z46;

    .line 170
    .line 171
    .line 172
    move-result-object v3

    .line 173
    invoke-virtual {v2, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 174
    .line 175
    .line 176
    :cond_af
    sget-object v3, Lf/ms5;->T01:Ljava/lang/String;

    .line 177
    .line 178
    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 179
    .line 180
    .line 181
    sget-object v3, Lf/cc4;->El1:Lf/x43;

    .line 182
    .line 183
    sget-object v7, Lf/x43;->instanceof:Lf/x43;

    .line 184
    .line 185
    if-ne v3, v7, :cond_e7

    .line 186
    .line 187
    sget-object v0, Lf/p37;->Uh0:Lf/o44;

    .line 188
    .line 189
    iget-object v0, v0, Lf/o44;->o31:Leu/pokemmo/client/AndroidLauncher;

    .line 190
    .line 191
    const-string v3, "android.permission.READ_EXTERNAL_STORAGE"

    .line 192
    .line 193
    invoke-static {v0, v3}, Lf/wt2;->UE0(Landroid/content/Context;Ljava/lang/String;)I

    .line 194
    .line 195
    .line 196
    move-result v0

    .line 197
    if-nez v0, :cond_e1

    .line 198
    .line 199
    sget-object v0, Lf/p37;->Uh0:Lf/o44;

    .line 200
    .line 201
    invoke-virtual {v0}, Lf/o44;->dq()Lf/z46;

    .line 202
    .line 203
    .line 204
    move-result-object v0

    .line 205
    const-string v3, "SD Directory {}"

    .line 206
    .line 207
    invoke-virtual {v0}, Lf/z46;->yJ()Ljava/lang/String;

    .line 208
    .line 209
    .line 210
    move-result-object v7

    .line 211
    invoke-interface {v5, v3, v7}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    .line 212
    .line 213
    .line 214
    invoke-virtual {v0}, Lf/z46;->A8()[Lf/z46;

    .line 215
    .line 216
    .line 217
    move-result-object v0

    .line 218
    invoke-static {v0}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    .line 219
    .line 220
    .line 221
    move-result-object v0

    .line 222
    :goto_dd
    invoke-virtual {v2, v0}, Ljava/util/ArrayList;->addAll(Ljava/util/Collection;)Z

    .line 223
    .line 224
    .line 225
    goto :goto_105

    .line 226
    :cond_e1
    const-string v0, "Read External Storage Permission not granted."

    .line 227
    .line 228
    invoke-interface {v5, v0}, Lf/xv7;->info(Ljava/lang/String;)V

    .line 229
    .line 230
    .line 231
    goto :goto_105

    .line 232
    :cond_e7
    invoke-virtual {v0}, Lf/rz;->hz1()Z

    .line 233
    .line 234
    .line 235
    move-result v3

    .line 236
    if-nez v3, :cond_f0

    .line 237
    .line 238
    invoke-virtual {v0}, Lf/z46;->kJ1()V

    .line 239
    .line 240
    .line 241
    :cond_f0
    invoke-virtual {v0}, Lf/rz;->hz1()Z

    .line 242
    .line 243
    .line 244
    move-result v3

    .line 245
    if-eqz v3, :cond_105

    .line 246
    .line 247
    invoke-virtual {v0}, Lf/rz;->mw0()Z

    .line 248
    .line 249
    .line 250
    move-result v3

    .line 251
    if-eqz v3, :cond_105

    .line 252
    .line 253
    invoke-virtual {v0}, Lf/rz;->A8()[Lf/z46;

    .line 254
    .line 255
    .line 256
    move-result-object v0

    .line 257
    invoke-static {v0}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    .line 258
    .line 259
    .line 260
    move-result-object v0

    .line 261
    goto :goto_dd

    .line 262
    :cond_105
    :goto_105
    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I

    .line 263
    .line 264
    .line 265
    move-result v0

    .line 266
    new-instance v3, Lf/bj0;

    .line 267
    .line 268
    const/4 v7, 0x5

    .line 269
    invoke-direct {v3, v7}, Lf/bj0;-><init>(I)V

    .line 270
    .line 271
    .line 272
    invoke-static {v2, v3}, Lj$/util/Collection$-EL;->removeIf(Ljava/util/Collection;Ljava/util/function/Predicate;)Z

    .line 273
    .line 274
    .line 275
    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I

    .line 276
    .line 277
    .line 278
    move-result v3

    .line 279
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 280
    .line 281
    .line 282
    move-result-object v3

    .line 283
    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I

    .line 284
    .line 285
    .line 286
    move-result v7

    .line 287
    sub-int/2addr v0, v7

    .line 288
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 289
    .line 290
    .line 291
    move-result-object v0

    .line 292
    const-string v7, "Rom potential count {} ({})"

    .line 293
    .line 294
    invoke-interface {v5, v7, v3, v0}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V

    .line 295
    .line 296
    .line 297
    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I

    .line 298
    .line 299
    .line 300
    move-result v3

    .line 301
    const/4 v0, 0x0

    .line 302
    :goto_12d
    const/4 v7, 0x1

    .line 303
    if-ge v0, v3, :cond_282

    .line 304
    .line 305
    invoke-virtual {v2, v0}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 306
    .line 307
    .line 308
    move-result-object v8

    .line 309
    add-int/lit8 v9, v0, 0x1

    .line 310
    .line 311
    check-cast v8, Lf/z46;

    .line 312
    .line 313
    if-eqz v8, :cond_14c

    .line 314
    .line 315
    invoke-virtual {v8}, Lf/z46;->hz1()Z

    .line 316
    .line 317
    .line 318
    move-result v0

    .line 319
    if-eqz v0, :cond_14c

    .line 320
    .line 321
    invoke-virtual {v8}, Lf/z46;->mw0()Z

    .line 322
    .line 323
    .line 324
    move-result v0

    .line 325
    if-nez v0, :cond_14c

    .line 326
    .line 327
    invoke-virtual {v8}, Lf/z46;->FK0()Ljava/io/File;

    .line 328
    .line 329
    .line 330
    move-result-object v0

    .line 331
    if-nez v0, :cond_14e

    .line 332
    .line 333
    :cond_14c
    :goto_14c
    move v0, v9

    .line 334
    goto :goto_12d

    .line 335
    :cond_14e
    iget-object v0, v8, Lf/z46;->O01:Ljava/io/File;

    .line 336
    .line 337
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 338
    .line 339
    .line 340
    move-result-object v0

    .line 341
    const-string v10, "importing.tmp"

    .line 342
    .line 343
    invoke-virtual {v0, v10}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 344
    .line 345
    .line 346
    move-result v0

    .line 347
    if-eqz v0, :cond_15d

    .line 348
    .line 349
    goto :goto_14c

    .line 350
    :cond_15d
    iget-object v0, v8, Lf/z46;->O01:Ljava/io/File;

    .line 351
    .line 352
    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    .line 353
    .line 354
    .line 355
    move-result-object v0

    .line 356
    invoke-virtual {v8}, Lf/z46;->HH()J

    .line 357
    .line 358
    .line 359
    move-result-wide v10

    .line 360
    const-wide/32 v12, 0x100000

    .line 361
    .line 362
    .line 363
    div-long/2addr v10, v12

    .line 364
    invoke-static {v10, v11}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    .line 365
    .line 366
    .line 367
    move-result-object v10

    .line 368
    invoke-virtual {v8}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 369
    .line 370
    .line 371
    move-result-object v11

    .line 372
    const/4 v12, 0x3

    .line 373
    new-array v13, v12, [Ljava/lang/Object;

    .line 374
    .line 375
    aput-object v0, v13, v4

    .line 376
    .line 377
    aput-object v10, v13, v7

    .line 378
    .line 379
    aput-object v11, v13, v6

    .line 380
    .line 381
    const-string v0, "Possible rom {} (size: {} MB) {}"

    .line 382
    .line 383
    invoke-interface {v5, v0, v13}, Lf/xv7;->info(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 384
    .line 385
    .line 386
    iget-object v0, p0, Lf/zw0;->JO0:Lf/th7;

    .line 387
    .line 388
    if-eqz v0, :cond_189

    .line 389
    .line 390
    iget-object v0, p0, Lf/zw0;->iT0:Lf/th7;

    .line 391
    .line 392
    if-nez v0, :cond_202

    .line 393
    .line 394
    :cond_189
    :try_start_189
    new-instance v0, Lf/th7;

    .line 395
    .line 396
    invoke-direct {v0, v8}, Lf/th7;-><init>(Lf/z46;)V

    .line 397
    .line 398
    .line 399
    invoke-virtual {v0}, Lf/th7;->sB1()B

    .line 400
    .line 401
    .line 402
    move-result v10

    .line 403
    if-nez v10, :cond_1ae

    .line 404
    .line 405
    iget-object v10, p0, Lf/zw0;->JO0:Lf/th7;

    .line 406
    .line 407
    if-nez v10, :cond_14c

    .line 408
    .line 409
    iget-object v10, v0, Lf/th7;->hP:Lf/e42;

    .line 410
    .line 411
    if-eqz v10, :cond_14c

    .line 412
    .line 413
    const-string v10, "Loaded {} as gba rom."

    .line 414
    .line 415
    invoke-virtual {v0}, Lf/th7;->S8()Ljava/lang/String;

    .line 416
    .line 417
    .line 418
    move-result-object v11

    .line 419
    invoke-interface {v5, v10, v11}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    .line 420
    .line 421
    .line 422
    iput-object v0, p0, Lf/zw0;->JO0:Lf/th7;

    .line 423
    .line 424
    goto :goto_14c

    .line 425
    :catch_1a8
    move-exception v0

    .line 426
    goto :goto_1c4

    .line 427
    :catch_1aa
    move-exception v0

    .line 428
    goto :goto_1d8

    .line 429
    :catch_1ac
    nop

    .line 430
    goto :goto_202

    .line 431
    :cond_1ae
    invoke-virtual {v0}, Lf/th7;->sB1()B

    .line 432
    .line 433
    .line 434
    move-result v10

    .line 435
    if-ne v10, v7, :cond_14c

    .line 436
    .line 437
    iget-object v10, p0, Lf/zw0;->iT0:Lf/th7;

    .line 438
    .line 439
    if-nez v10, :cond_14c

    .line 440
    .line 441
    const-string v10, "Loaded {} as extended gba rom."

    .line 442
    .line 443
    invoke-virtual {v0}, Lf/th7;->S8()Ljava/lang/String;

    .line 444
    .line 445
    .line 446
    move-result-object v11

    .line 447
    invoke-interface {v5, v10, v11}, Lf/xv7;->info(Ljava/lang/String;Ljava/lang/Object;)V

    .line 448
    .line 449
    .line 450
    iput-object v0, p0, Lf/zw0;->iT0:Lf/th7;
    :try_end_1c3
    .catch Lf/gz5; {:try_start_189 .. :try_end_1c3} :catch_1ac
    .catch Ljava/io/IOException; {:try_start_189 .. :try_end_1c3} :catch_1aa
    .catch Ljava/lang/Exception; {:try_start_189 .. :try_end_1c3} :catch_1a8

    .line 451
    .line 452
    goto :goto_14c

    .line 453
    :goto_1c4
    invoke-virtual {v8}, Lf/z46;->yJ()Ljava/lang/String;

    .line 454
    .line 455
    .line 456
    move-result-object v10

    .line 457
    invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    .line 458
    .line 459
    .line 460
    move-result-object v11

    .line 461
    new-array v13, v12, [Ljava/lang/Object;

    .line 462
    .line 463
    aput-object v10, v13, v4

    .line 464
    .line 465
    aput-object v11, v13, v7

    .line 466
    .line 467
    aput-object v0, v13, v6

    .line 468
    .line 469
    invoke-interface {v5, v1, v13}, Lf/xv7;->warn(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 470
    .line 471
    .line 472
    goto :goto_202

    .line 473
    :goto_1d8
    invoke-virtual {v8}, Lf/z46;->yJ()Ljava/lang/String;

    .line 474
    .line 475
    .line 476
    move-result-object v10

    .line 477
    invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    .line 478
    .line 479
    .line 480
    move-result-object v11

    .line 481
    new-array v13, v12, [Ljava/lang/Object;

    .line 482
    .line 483
    aput-object v10, v13, v4

    .line 484
    .line 485
    aput-object v11, v13, v7

    .line 486
    .line 487
    aput-object v0, v13, v6

    .line 488
    .line 489
    invoke-interface {v5, v1, v13}, Lf/xv7;->warn(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 490
    .line 491
    .line 492
    sget-object v10, Lf/p37;->T10:Lf/zw0;

    .line 493
    .line 494
    invoke-virtual {v10}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 495
    .line 496
    .line 497
    invoke-static {v0}, Lf/zw0;->ls(Ljava/lang/Exception;)Z

    .line 498
    .line 499
    .line 500
    move-result v0

    .line 501
    if-eqz v0, :cond_202

    .line 502
    .line 503
    :goto_1f6
    iput-boolean v7, p0, Lf/zw0;->yf:Z

    .line 504
    .line 505
    sget-object v0, Lf/p37;->Vh0:Lf/i72;

    .line 506
    .line 507
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 508
    .line 509
    .line 510
    invoke-static {}, Lf/i72;->si()V

    .line 511
    .line 512
    .line 513
    goto/16 :goto_2f8

    .line 514
    .line 515
    :cond_202
    :goto_202
    invoke-virtual {v8}, Lf/z46;->HH()J

    .line 516
    .line 517
    .line 518
    move-result-wide v10

    .line 519
    const-wide/16 v13, 0x800

    .line 520
    .line 521
    cmp-long v0, v10, v13

    .line 522
    .line 523
    if-lez v0, :cond_14c

    .line 524
    .line 525
    :try_start_20c
    new-instance v0, Lf/xw0;

    .line 526
    .line 527
    invoke-direct {v0, v8}, Lf/xw0;-><init>(Lf/z46;)V

    .line 528
    .line 529
    .line 530
    iget-object v0, v0, Lf/xw0;->tW1:Ljava/lang/String;

    .line 531
    .line 532
    iget-object v10, p0, Lf/zw0;->pH1:Lf/v67;

    .line 533
    .line 534
    if-nez v10, :cond_22d

    .line 535
    .line 536
    sget-object v10, Lf/v67;->Gy:[Ljava/lang/String;

    .line 537
    .line 538
    invoke-static {v10}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    .line 539
    .line 540
    .line 541
    move-result-object v10

    .line 542
    invoke-interface {v10, v0}, Ljava/util/List;->contains(Ljava/lang/Object;)Z

    .line 543
    .line 544
    .line 545
    move-result v10

    .line 546
    if-eqz v10, :cond_22d

    .line 547
    .line 548
    new-instance v10, Lf/v67;

    .line 549
    .line 550
    invoke-direct {v10, v8}, Lf/v67;-><init>(Lf/z46;)V

    .line 551
    .line 552
    .line 553
    iput-object v10, p0, Lf/zw0;->pH1:Lf/v67;

    .line 554
    .line 555
    goto :goto_22d

    .line 556
    :catch_22b
    move-exception v0

    .line 557
    goto :goto_260

    .line 558
    :cond_22d
    :goto_22d
    iget-object v10, p0, Lf/zw0;->WP1:Lf/rf3;

    .line 559
    .line 560
    if-nez v10, :cond_244

    .line 561
    .line 562
    sget-object v10, Lf/rf3;->Cm0:[Ljava/lang/String;

    .line 563
    .line 564
    invoke-static {v10}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    .line 565
    .line 566
    .line 567
    move-result-object v10

    .line 568
    invoke-interface {v10, v0}, Ljava/util/List;->contains(Ljava/lang/Object;)Z

    .line 569
    .line 570
    .line 571
    move-result v10

    .line 572
    if-eqz v10, :cond_244

    .line 573
    .line 574
    new-instance v10, Lf/rf3;

    .line 575
    .line 576
    invoke-direct {v10, v8}, Lf/rf3;-><init>(Lf/z46;)V

    .line 577
    .line 578
    .line 579
    iput-object v10, p0, Lf/zw0;->WP1:Lf/rf3;

    .line 580
    .line 581
    :cond_244
    iget-object v10, p0, Lf/zw0;->lW0:Lf/l91;

    .line 582
    .line 583
    if-nez v10, :cond_14c

    .line 584
    .line 585
    sget-object v10, Lf/l91;->yl0:[Ljava/lang/String;

    .line 586
    .line 587
    invoke-static {v10}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    .line 588
    .line 589
    .line 590
    move-result-object v10

    .line 591
    invoke-interface {v10, v0}, Ljava/util/List;->contains(Ljava/lang/Object;)Z

    .line 592
    .line 593
    .line 594
    move-result v0

    .line 595
    if-eqz v0, :cond_14c

    .line 596
    .line 597
    new-instance v0, Lf/l91;

    .line 598
    .line 599
    invoke-direct {v0, v8}, Lf/l91;-><init>(Lf/z46;)V

    .line 600
    .line 601
    .line 602
    iput-object v0, p0, Lf/zw0;->lW0:Lf/l91;
    :try_end_25b
    .catch Lf/gz5; {:try_start_20c .. :try_end_25b} :catch_25d
    .catch Ljava/lang/Exception; {:try_start_20c .. :try_end_25b} :catch_22b

    .line 603
    .line 604
    goto/16 :goto_14c

    .line 605
    .line 606
    :catch_25d
    nop

    .line 607
    goto/16 :goto_14c

    .line 608
    .line 609
    :goto_260
    invoke-virtual {v8}, Lf/z46;->yJ()Ljava/lang/String;

    .line 610
    .line 611
    .line 612
    move-result-object v8

    .line 613
    invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    .line 614
    .line 615
    .line 616
    move-result-object v10

    .line 617
    new-array v11, v12, [Ljava/lang/Object;

    .line 618
    .line 619
    aput-object v8, v11, v4

    .line 620
    .line 621
    aput-object v10, v11, v7

    .line 622
    .line 623
    aput-object v0, v11, v6

    .line 624
    .line 625
    const-string v8, "Could not load {} as a nds rom because: {}"

    .line 626
    .line 627
    invoke-interface {v5, v8, v11}, Lf/xv7;->warn(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 628
    .line 629
    .line 630
    sget-object v8, Lf/p37;->T10:Lf/zw0;

    .line 631
    .line 632
    invoke-virtual {v8}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 633
    .line 634
    .line 635
    invoke-static {v0}, Lf/zw0;->ls(Ljava/lang/Exception;)Z

    .line 636
    .line 637
    .line 638
    move-result v0

    .line 639
    if-eqz v0, :cond_14c

    .line 640
    .line 641
    goto/16 :goto_1f6

    .line 642
    .line 643
    :cond_282
    iget-object v0, p0, Lf/zw0;->pH1:Lf/v67;

    .line 644
    .line 645
    if-nez v0, :cond_289

    .line 646
    .line 647
    iput-boolean v7, p0, Lf/zw0;->yf:Z

    .line 648
    .line 649
    goto :goto_2f8

    .line 650
    :cond_289
    new-instance v0, Ljava/util/ArrayList;

    .line 651
    .line 652
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 653
    .line 654
    .line 655
    iget-object v1, p0, Lf/zw0;->JO0:Lf/th7;

    .line 656
    .line 657
    if-eqz v1, :cond_295

    .line 658
    .line 659
    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 660
    .line 661
    .line 662
    :cond_295
    iget-object v1, p0, Lf/zw0;->iT0:Lf/th7;

    .line 663
    .line 664
    if-eqz v1, :cond_29c

    .line 665
    .line 666
    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 667
    .line 668
    .line 669
    :cond_29c
    new-array v1, v4, [Lf/th7;

    .line 670
    .line 671
    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;

    .line 672
    .line 673
    .line 674
    move-result-object v0

    .line 675
    check-cast v0, [Lf/th7;

    .line 676
    .line 677
    iput-object v0, p0, Lf/zw0;->aR1:[Lf/th7;

    .line 678
    .line 679
    new-instance v0, Ljava/util/ArrayList;

    .line 680
    .line 681
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 682
    .line 683
    .line 684
    iget-object v1, p0, Lf/zw0;->pH1:Lf/v67;

    .line 685
    .line 686
    if-eqz v1, :cond_2b2

    .line 687
    .line 688
    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 689
    .line 690
    .line 691
    :cond_2b2
    iget-object v1, p0, Lf/zw0;->lW0:Lf/l91;

    .line 692
    .line 693
    if-eqz v1, :cond_2b9

    .line 694
    .line 695
    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 696
    .line 697
    .line 698
    :cond_2b9
    iget-object v1, p0, Lf/zw0;->WP1:Lf/rf3;

    .line 699
    .line 700
    if-eqz v1, :cond_2c0

    .line 701
    .line 702
    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 703
    .line 704
    .line 705
    :cond_2c0
    new-array v1, v4, [Lf/sg7;

    .line 706
    .line 707
    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;

    .line 708
    .line 709
    .line 710
    move-result-object v0

    .line 711
    check-cast v0, [Lf/sg7;

    .line 712
    .line 713
    iput-object v0, p0, Lf/zw0;->CU:[Lf/sg7;

    .line 714
    .line 715
    new-instance v0, Lf/fu4;

    .line 716
    .line 717
    invoke-direct {v0}, Lf/cu2;-><init>()V

    .line 718
    .line 719
    .line 720
    iget-object v1, p0, Lf/zw0;->aR1:[Lf/th7;

    .line 721
    .line 722
    array-length v2, v1

    .line 723
    const/4 v3, 0x0

    .line 724
    :goto_2d3
    if-ge v3, v2, :cond_2e1

    .line 725
    .line 726
    aget-object v5, v1, v3

    .line 727
    .line 728
    invoke-virtual {v5}, Lf/th7;->sB1()B

    .line 729
    .line 730
    .line 731
    move-result v5

    .line 732
    invoke-virtual {v0, v5}, Lf/fu4;->yu1(B)Z

    .line 733
    .line 734
    .line 735
    add-int/lit8 v3, v3, 0x1

    .line 736
    .line 737
    goto :goto_2d3

    .line 738
    :cond_2e1
    iget-object v1, p0, Lf/zw0;->CU:[Lf/sg7;

    .line 739
    .line 740
    array-length v2, v1

    .line 741
    :goto_2e4
    if-ge v4, v2, :cond_2f2

    .line 742
    .line 743
    aget-object v3, v1, v4

    .line 744
    .line 745
    invoke-virtual {v3}, Lf/sg7;->Hb()B

    .line 746
    .line 747
    .line 748
    move-result v3

    .line 749
    invoke-virtual {v0, v3}, Lf/fu4;->yu1(B)Z

    .line 750
    .line 751
    .line 752
    add-int/lit8 v4, v4, 0x1

    .line 753
    .line 754
    goto :goto_2e4

    .line 755
    :cond_2f2
    invoke-virtual {v0}, Lf/fu4;->WH()[B

    .line 756
    .line 757
    .line 758
    move-result-object v0

    .line 759
    iput-object v0, p0, Lf/zw0;->i9:[B

    .line 760
    .line 761
    :goto_2f8
    return-void
.end method
