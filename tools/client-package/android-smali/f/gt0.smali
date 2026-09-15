.class public abstract Lf/gt0;
.super Ljava/lang/Object;


# static fields
.field public static final LPT1:Ljava/util/EnumMap;

.field public static final Po0:[[[I

.field public static final ZT:[[[I

.field public static final Zq1:[[[I

.field public static final b91:[[Lf/ua7;

.field public static final bJ1:Lf/k89;

.field public static final rC0:Lf/jy2;

.field public static final t3:[Ljava/lang/String;


# direct methods
.method static constructor <clinit>()V
    .registers 18

    .line 1
    const/4 v0, 0x0

    .line 2
    new-array v1, v0, [Ljava/lang/String;

    .line 3
    .line 4
    sput-object v1, Lf/gt0;->t3:[Ljava/lang/String;

    .line 5
    .line 6
    new-instance v1, Ljava/util/EnumMap;

    .line 7
    .line 8
    const-class v2, Lf/kx5;

    .line 9
    .line 10
    invoke-direct {v1, v2}, Ljava/util/EnumMap;-><init>(Ljava/lang/Class;)V

    .line 11
    .line 12
    .line 13
    sput-object v1, Lf/gt0;->LPT1:Ljava/util/EnumMap;

    .line 14
    .line 15
    new-array v1, v0, [[I

    .line 16
    .line 17
    const/4 v2, 0x2

    .line 18
    new-array v3, v2, [[I

    .line 19
    .line 20
    const/16 v4, 0x40

    .line 21
    .line 22
    filled-new-array {v4, v2}, [I

    .line 23
    .line 24
    .line 25
    move-result-object v5

    .line 26
    aput-object v5, v3, v0

    .line 27
    .line 28
    const/16 v5, 0xc

    .line 29
    .line 30
    const/16 v6, 0x17

    .line 31
    .line 32
    filled-new-array {v5, v6}, [I

    .line 33
    .line 34
    .line 35
    move-result-object v5

    .line 36
    const/4 v6, 0x1

    .line 37
    aput-object v5, v3, v6

    .line 38
    .line 39
    new-array v5, v2, [[I

    .line 40
    .line 41
    const/4 v7, 0x3

    .line 42
    filled-new-array {v4, v7}, [I

    .line 43
    .line 44
    .line 45
    move-result-object v8

    .line 46
    aput-object v8, v5, v0

    .line 47
    .line 48
    const/16 v8, 0x18

    .line 49
    .line 50
    filled-new-array {v8, v7}, [I

    .line 51
    .line 52
    .line 53
    move-result-object v8

    .line 54
    aput-object v8, v5, v6

    .line 55
    .line 56
    new-array v8, v2, [[I

    .line 57
    .line 58
    const/4 v9, 0x4

    .line 59
    filled-new-array {v4, v9}, [I

    .line 60
    .line 61
    .line 62
    move-result-object v10

    .line 63
    aput-object v10, v8, v0

    .line 64
    .line 65
    const/16 v10, 0x22

    .line 66
    .line 67
    filled-new-array {v10, v7}, [I

    .line 68
    .line 69
    .line 70
    move-result-object v10

    .line 71
    aput-object v10, v8, v6

    .line 72
    .line 73
    new-array v10, v2, [[I

    .line 74
    .line 75
    const/4 v11, 0x5

    .line 76
    filled-new-array {v4, v11}, [I

    .line 77
    .line 78
    .line 79
    move-result-object v12

    .line 80
    aput-object v12, v10, v0

    .line 81
    .line 82
    const/16 v12, 0x44

    .line 83
    .line 84
    filled-new-array {v12, v7}, [I

    .line 85
    .line 86
    .line 87
    move-result-object v12

    .line 88
    aput-object v12, v10, v6

    .line 89
    .line 90
    new-array v12, v2, [[I

    .line 91
    .line 92
    const/4 v13, 0x6

    .line 93
    filled-new-array {v4, v13}, [I

    .line 94
    .line 95
    .line 96
    move-result-object v14

    .line 97
    aput-object v14, v12, v0

    .line 98
    .line 99
    const/16 v14, 0x66

    .line 100
    .line 101
    filled-new-array {v14, v7}, [I

    .line 102
    .line 103
    .line 104
    move-result-object v14

    .line 105
    aput-object v14, v12, v6

    .line 106
    .line 107
    new-array v14, v2, [[I

    .line 108
    .line 109
    const/4 v15, 0x7

    .line 110
    filled-new-array {v4, v15}, [I

    .line 111
    .line 112
    .line 113
    move-result-object v16

    .line 114
    aput-object v16, v14, v0

    .line 115
    .line 116
    const/16 v16, 0x4

    .line 117
    .line 118
    const/16 v9, 0x71

    .line 119
    .line 120
    filled-new-array {v9, v7}, [I

    .line 121
    .line 122
    .line 123
    move-result-object v9

    .line 124
    aput-object v9, v14, v6

    .line 125
    .line 126
    new-array v9, v2, [[I

    .line 127
    .line 128
    const/16 v17, 0x5

    .line 129
    .line 130
    const/16 v11, 0x8

    .line 131
    .line 132
    filled-new-array {v4, v11}, [I

    .line 133
    .line 134
    .line 135
    move-result-object v4

    .line 136
    aput-object v4, v9, v0

    .line 137
    .line 138
    const/16 v4, 0x77

    .line 139
    .line 140
    filled-new-array {v4, v7}, [I

    .line 141
    .line 142
    .line 143
    move-result-object v4

    .line 144
    aput-object v4, v9, v6

    .line 145
    .line 146
    new-array v4, v11, [[[I

    .line 147
    .line 148
    aput-object v1, v4, v0

    .line 149
    .line 150
    aput-object v3, v4, v6

    .line 151
    .line 152
    aput-object v5, v4, v2

    .line 153
    .line 154
    aput-object v8, v4, v7

    .line 155
    .line 156
    aput-object v10, v4, v16

    .line 157
    .line 158
    aput-object v12, v4, v17

    .line 159
    .line 160
    aput-object v14, v4, v13

    .line 161
    .line 162
    aput-object v9, v4, v15

    .line 163
    .line 164
    sput-object v4, Lf/gt0;->Po0:[[[I

    .line 165
    .line 166
    new-array v1, v0, [[I

    .line 167
    .line 168
    new-array v3, v0, [[I

    .line 169
    .line 170
    new-array v4, v2, [[I

    .line 171
    .line 172
    const/16 v5, 0x57

    .line 173
    .line 174
    filled-new-array {v5, v7}, [I

    .line 175
    .line 176
    .line 177
    move-result-object v5

    .line 178
    aput-object v5, v4, v0

    .line 179
    .line 180
    const/16 v5, 0x58

    .line 181
    .line 182
    filled-new-array {v5, v7}, [I

    .line 183
    .line 184
    .line 185
    move-result-object v5

    .line 186
    aput-object v5, v4, v6

    .line 187
    .line 188
    new-array v5, v0, [[I

    .line 189
    .line 190
    new-array v8, v6, [[I

    .line 191
    .line 192
    const/16 v9, 0x90

    .line 193
    .line 194
    filled-new-array {v9, v7}, [I

    .line 195
    .line 196
    .line 197
    move-result-object v9

    .line 198
    aput-object v9, v8, v0

    .line 199
    .line 200
    new-array v9, v0, [[I

    .line 201
    .line 202
    new-array v10, v6, [[I

    .line 203
    .line 204
    const/16 v12, 0x38

    .line 205
    .line 206
    filled-new-array {v12, v7}, [I

    .line 207
    .line 208
    .line 209
    move-result-object v12

    .line 210
    aput-object v12, v10, v0

    .line 211
    .line 212
    new-array v12, v0, [[I

    .line 213
    .line 214
    new-array v14, v11, [[[I

    .line 215
    .line 216
    aput-object v1, v14, v0

    .line 217
    .line 218
    aput-object v3, v14, v6

    .line 219
    .line 220
    aput-object v4, v14, v2

    .line 221
    .line 222
    aput-object v5, v14, v7

    .line 223
    .line 224
    aput-object v8, v14, v16

    .line 225
    .line 226
    aput-object v9, v14, v17

    .line 227
    .line 228
    aput-object v10, v14, v13

    .line 229
    .line 230
    aput-object v12, v14, v15

    .line 231
    .line 232
    sput-object v14, Lf/gt0;->ZT:[[[I

    .line 233
    .line 234
    new-array v1, v0, [[I

    .line 235
    .line 236
    new-array v3, v6, [[I

    .line 237
    .line 238
    const/16 v4, 0x22e

    .line 239
    .line 240
    filled-new-array {v4, v7}, [I

    .line 241
    .line 242
    .line 243
    move-result-object v4

    .line 244
    aput-object v4, v3, v0

    .line 245
    .line 246
    new-array v4, v6, [[I

    .line 247
    .line 248
    const/16 v5, 0x237

    .line 249
    .line 250
    filled-new-array {v5, v7}, [I

    .line 251
    .line 252
    .line 253
    move-result-object v5

    .line 254
    aput-object v5, v4, v0

    .line 255
    .line 256
    new-array v5, v0, [[I

    .line 257
    .line 258
    new-array v8, v6, [[I

    .line 259
    .line 260
    const/16 v9, 0x266

    .line 261
    .line 262
    filled-new-array {v9, v7}, [I

    .line 263
    .line 264
    .line 265
    move-result-object v9

    .line 266
    aput-object v9, v8, v0

    .line 267
    .line 268
    new-array v9, v0, [[I

    .line 269
    .line 270
    new-array v10, v6, [[I

    .line 271
    .line 272
    const/16 v12, 0x25e

    .line 273
    .line 274
    filled-new-array {v12, v7}, [I

    .line 275
    .line 276
    .line 277
    move-result-object v12

    .line 278
    aput-object v12, v10, v0

    .line 279
    .line 280
    new-array v12, v0, [[I

    .line 281
    .line 282
    new-array v11, v11, [[[I

    .line 283
    .line 284
    aput-object v1, v11, v0

    .line 285
    .line 286
    aput-object v3, v11, v6

    .line 287
    .line 288
    aput-object v4, v11, v2

    .line 289
    .line 290
    aput-object v5, v11, v7

    .line 291
    .line 292
    aput-object v8, v11, v16

    .line 293
    .line 294
    aput-object v9, v11, v17

    .line 295
    .line 296
    aput-object v10, v11, v13

    .line 297
    .line 298
    aput-object v12, v11, v15

    .line 299
    .line 300
    sput-object v11, Lf/gt0;->Zq1:[[[I

    .line 301
    .line 302
    new-instance v1, Lf/k89;

    .line 303
    .line 304
    invoke-direct {v1}, Lf/x44;-><init>()V

    .line 305
    .line 306
    .line 307
    sput-object v1, Lf/gt0;->bJ1:Lf/k89;

    .line 308
    .line 309
    new-instance v1, Lf/jy2;

    .line 310
    .line 311
    invoke-direct {v1}, Lf/x44;-><init>()V

    .line 312
    .line 313
    .line 314
    sput-object v1, Lf/gt0;->rC0:Lf/jy2;

    .line 315
    .line 316
    new-array v1, v2, [I

    .line 317
    .line 318
    aput v2, v1, v6

    .line 319
    .line 320
    aput v17, v1, v0

    .line 321
    .line 322
    const-class v3, Lf/ua7;

    .line 323
    .line 324
    invoke-static {v3, v1}, Ljava/lang/reflect/Array;->newInstance(Ljava/lang/Class;[I)Ljava/lang/Object;

    .line 325
    .line 326
    .line 327
    move-result-object v1

    .line 328
    check-cast v1, [[Lf/ua7;

    .line 329
    .line 330
    sput-object v1, Lf/gt0;->b91:[[Lf/ua7;

    .line 331
    .line 332
    const/4 v1, 0x0

    .line 333
    :goto_14c
    sget-object v3, Lf/gt0;->b91:[[Lf/ua7;

    .line 334
    .line 335
    array-length v3, v3

    .line 336
    if-ge v1, v3, :cond_162

    .line 337
    .line 338
    const/4 v3, 0x0

    .line 339
    :goto_152
    if-ge v3, v2, :cond_15f

    .line 340
    .line 341
    sget-object v4, Lf/gt0;->b91:[[Lf/ua7;

    .line 342
    .line 343
    aget-object v4, v4, v1

    .line 344
    .line 345
    sget-object v5, Lf/ua7;->c3:Lf/ua7;

    .line 346
    .line 347
    aput-object v5, v4, v3

    .line 348
    .line 349
    add-int/lit8 v3, v3, 0x1

    .line 350
    .line 351
    goto :goto_152

    .line 352
    :cond_15f
    add-int/lit8 v1, v1, 0x1

    .line 353
    .line 354
    goto :goto_14c

    .line 355
    :cond_162
    invoke-static {}, Lf/kx5;->values()[Lf/kx5;

    .line 356
    .line 357
    .line 358
    move-result-object v1

    .line 359
    array-length v2, v1

    .line 360
    :goto_167
    if-ge v0, v2, :cond_175

    .line 361
    .line 362
    aget-object v3, v1, v0

    .line 363
    .line 364
    sget-object v4, Lf/gt0;->LPT1:Ljava/util/EnumMap;

    .line 365
    .line 366
    const-string v5, ""

    .line 367
    .line 368
    invoke-virtual {v4, v3, v5}, Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;

    .line 369
    .line 370
    .line 371
    add-int/lit8 v0, v0, 0x1

    .line 372
    .line 373
    goto :goto_167

    .line 374
    :cond_175
    sget-object v0, Lf/j80;->kr0:Lf/xv7;

    .line 375
    .line 376
    const-string v1, "Added string preloads..."

    .line 377
    .line 378
    invoke-interface {v0, v1}, Lf/xv7;->info(Ljava/lang/String;)V

    .line 379
    .line 380
    .line 381
    return-void
.end method

.method public static AZ1(I[Ljava/lang/String;)V
    .registers 5

    .line 1
    const/4 v0, 0x0

    .line 2
    :goto_1
    array-length v1, p1

    .line 3
    if-ge v0, v1, :cond_e

    .line 4
    .line 5
    add-int v1, p0, v0

    .line 6
    .line 7
    aget-object v2, p1, v0

    .line 8
    .line 9
    invoke-static {v1, v2}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 10
    .line 11
    .line 12
    add-int/lit8 v0, v0, 0x1

    .line 13
    .line 14
    goto :goto_1

    .line 15
    :cond_e
    return-void
.end method

.method public static CS(IILjava/nio/ByteBuffer;I)V
    .registers 7

    .line 1
    const/4 v0, 0x1

    .line 2
    if-ge p0, v0, :cond_4

    .line 3
    .line 4
    goto :goto_2c

    .line 5
    :cond_4
    invoke-virtual {p2, p0}, Ljava/nio/ByteBuffer;->position(I)Ljava/nio/Buffer;

    .line 6
    .line 7
    .line 8
    new-array p0, p1, [B

    .line 9
    .line 10
    const/4 p1, 0x0

    .line 11
    const/4 v0, 0x0

    .line 12
    :goto_b
    const v1, 0x7fffffff

    .line 13
    .line 14
    .line 15
    if-ge v0, v1, :cond_2c

    .line 16
    .line 17
    invoke-virtual {p2, p0}, Ljava/nio/ByteBuffer;->get([B)Ljava/nio/ByteBuffer;

    .line 18
    .line 19
    .line 20
    aget-byte v1, p0, p1

    .line 21
    .line 22
    const/4 v2, -0x1

    .line 23
    if-eq v1, v2, :cond_2c

    .line 24
    .line 25
    if-nez v1, :cond_1b

    .line 26
    .line 27
    goto :goto_2c

    .line 28
    :cond_1b
    invoke-static {p0}, Lf/x25;->Xs([B)Ljava/lang/String;

    .line 29
    .line 30
    .line 31
    move-result-object v1

    .line 32
    invoke-static {v1}, Lf/ay0;->bJ0(Ljava/lang/String;)Ljava/lang/String;

    .line 33
    .line 34
    .line 35
    move-result-object v1

    .line 36
    add-int/lit8 v2, p3, 0x1

    .line 37
    .line 38
    invoke-static {p3, v1}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 39
    .line 40
    .line 41
    add-int/lit8 v0, v0, 0x1

    .line 42
    .line 43
    move p3, v2

    .line 44
    goto :goto_b

    .line 45
    :cond_2c
    :goto_2c
    return-void
.end method

.method public static Com5(ILjava/lang/String;)V
    .registers 3

    .line 1
    sget-object v0, Lf/gt0;->bJ1:Lf/k89;

    .line 2
    .line 3
    invoke-virtual {v0, p0, p1}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    .line 4
    .line 5
    .line 6
    return-void
.end method

.method public static Dz()V
    .registers 17

    .line 1
    sget-object v0, Lf/pn2;->lC1:Lf/pn2;

    .line 2
    .line 3
    const/4 v1, 0x2

    .line 4
    invoke-static {v1, v0}, Lf/gt0;->NP(BLf/pn2;)Lf/ua7;

    .line 5
    .line 6
    .line 7
    move-result-object v0

    .line 8
    sget-object v2, Lf/pn2;->SD:Lf/pn2;

    .line 9
    .line 10
    invoke-static {v1, v2}, Lf/gt0;->NP(BLf/pn2;)Lf/ua7;

    .line 11
    .line 12
    .line 13
    move-result-object v2

    .line 14
    iget-boolean v3, v2, Lf/ua7;->HV:Z

    .line 15
    .line 16
    const/4 v4, 0x0

    .line 17
    if-eqz v3, :cond_15

    .line 18
    .line 19
    sget v3, Lf/ua7;->J61:I

    .line 20
    .line 21
    goto :goto_16

    .line 22
    :cond_15
    const/4 v3, 0x0

    .line 23
    :goto_16
    const/4 v5, 0x0

    .line 24
    :goto_17
    const/4 v6, 0x0

    .line 25
    const/16 v7, 0x13a8

    .line 26
    .line 27
    const/16 v8, 0x13a7

    .line 28
    .line 29
    const/16 v9, 0x13a6

    .line 30
    .line 31
    const/16 v10, 0x13a9

    .line 32
    .line 33
    const/16 v11, 0x8

    .line 34
    .line 35
    const-string v12, "[0-9\uff10-\uff19,]{2,10}(?!\\})"

    .line 36
    .line 37
    const/4 v13, 0x1

    .line 38
    if-ge v5, v1, :cond_130

    .line 39
    .line 40
    const/16 v14, 0x6e

    .line 41
    .line 42
    invoke-virtual {v0, v14, v5, v1}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 43
    .line 44
    .line 45
    move-result-object v15

    .line 46
    invoke-static {v15, v10}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 47
    .line 48
    .line 49
    move-result-object v10

    .line 50
    invoke-virtual {v0, v14, v5, v10, v1}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 51
    .line 52
    .line 53
    const/16 v10, 0x1d0

    .line 54
    .line 55
    const/4 v14, 0x6

    .line 56
    invoke-virtual {v0, v10, v5, v14}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 57
    .line 58
    .line 59
    move-result-object v15

    .line 60
    invoke-static {v15, v9}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 61
    .line 62
    .line 63
    move-result-object v9

    .line 64
    invoke-virtual {v0, v10, v5, v9, v14}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 65
    .line 66
    .line 67
    const/4 v9, 0x7

    .line 68
    invoke-virtual {v0, v10, v5, v9}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 69
    .line 70
    .line 71
    move-result-object v14

    .line 72
    invoke-static {v14, v8}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 73
    .line 74
    .line 75
    move-result-object v8

    .line 76
    invoke-virtual {v0, v10, v5, v8, v9}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 77
    .line 78
    .line 79
    invoke-virtual {v0, v10, v5, v11}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 80
    .line 81
    .line 82
    move-result-object v8

    .line 83
    invoke-static {v8, v7}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 84
    .line 85
    .line 86
    move-result-object v7

    .line 87
    invoke-virtual {v0, v10, v5, v7, v11}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 88
    .line 89
    .line 90
    const/16 v7, 0x26

    .line 91
    .line 92
    const/16 v8, 0x9

    .line 93
    .line 94
    invoke-virtual {v0, v7, v5, v8}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 95
    .line 96
    .line 97
    move-result-object v9

    .line 98
    const/16 v10, 0x15d7

    .line 99
    .line 100
    invoke-static {v9, v10}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 101
    .line 102
    .line 103
    move-result-object v9

    .line 104
    invoke-virtual {v0, v7, v5, v9, v8}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 105
    .line 106
    .line 107
    sget-object v7, Lf/an8;->LU:Lf/an8;

    .line 108
    .line 109
    iget-object v7, v7, Lf/an8;->lO:Ljava/util/TreeMap;

    .line 110
    .line 111
    invoke-virtual {v7}, Ljava/util/TreeMap;->values()Ljava/util/Collection;

    .line 112
    .line 113
    .line 114
    move-result-object v7

    .line 115
    invoke-interface {v7}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    .line 116
    .line 117
    .line 118
    move-result-object v7

    .line 119
    :cond_76
    :goto_76
    invoke-interface {v7}, Ljava/util/Iterator;->hasNext()Z

    .line 120
    .line 121
    .line 122
    move-result v8

    .line 123
    if-eqz v8, :cond_fa

    .line 124
    .line 125
    invoke-interface {v7}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 126
    .line 127
    .line 128
    move-result-object v8

    .line 129
    check-cast v8, Lf/ls0;

    .line 130
    .line 131
    iget-byte v9, v8, Lf/ls0;->Lg0:B

    .line 132
    .line 133
    if-eq v9, v1, :cond_87

    .line 134
    .line 135
    goto :goto_76

    .line 136
    :cond_87
    iget-short v9, v8, Lf/ls0;->Bm0:S

    .line 137
    .line 138
    const-string v11, ""

    .line 139
    .line 140
    const/16 v14, 0x35

    .line 141
    .line 142
    if-lez v9, :cond_c4

    .line 143
    .line 144
    iget-boolean v9, v8, Lf/ls0;->y3:Z

    .line 145
    .line 146
    if-nez v9, :cond_c4

    .line 147
    .line 148
    iget-short v9, v8, Lf/ls0;->C4:S

    .line 149
    .line 150
    add-int/lit16 v9, v9, -0x1388

    .line 151
    .line 152
    invoke-virtual {v2, v14, v5, v9}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 153
    .line 154
    .line 155
    move-result-object v9

    .line 156
    new-instance v15, Ljava/lang/StringBuilder;

    .line 157
    .line 158
    invoke-direct {v15}, Ljava/lang/StringBuilder;-><init>()V

    .line 159
    .line 160
    .line 161
    const v16, 0x1fbd0

    .line 162
    .line 163
    .line 164
    iget-short v10, v8, Lf/ls0;->Bm0:S

    .line 165
    .line 166
    invoke-virtual {v15, v10}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 167
    .line 168
    .line 169
    invoke-virtual {v15, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 170
    .line 171
    .line 172
    invoke-virtual {v15}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 173
    .line 174
    .line 175
    move-result-object v10

    .line 176
    invoke-static {v9, v10, v12}, Lf/gt0;->aH1(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 177
    .line 178
    .line 179
    move-result-object v9

    .line 180
    iget-short v10, v8, Lf/ls0;->C4:S

    .line 181
    .line 182
    add-int/lit16 v10, v10, -0x1388

    .line 183
    .line 184
    invoke-virtual {v2, v14, v5, v9, v10}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 185
    .line 186
    .line 187
    if-ne v5, v3, :cond_c7

    .line 188
    .line 189
    iget-short v10, v8, Lf/ls0;->C4:S

    .line 190
    .line 191
    add-int v10, v10, v16

    .line 192
    .line 193
    invoke-static {v10, v9}, Lf/gt0;->Com5(ILjava/lang/String;)V

    .line 194
    .line 195
    .line 196
    goto :goto_c7

    .line 197
    :cond_c4
    const v16, 0x1fbd0

    .line 198
    .line 199
    .line 200
    :cond_c7
    :goto_c7
    iget-byte v9, v8, Lf/ls0;->Nz:B

    .line 201
    .line 202
    if-lez v9, :cond_76

    .line 203
    .line 204
    iget-short v9, v8, Lf/ls0;->C4:S

    .line 205
    .line 206
    add-int/lit16 v9, v9, -0x1388

    .line 207
    .line 208
    invoke-virtual {v2, v14, v5, v9}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 209
    .line 210
    .line 211
    move-result-object v9

    .line 212
    new-instance v10, Ljava/lang/StringBuilder;

    .line 213
    .line 214
    invoke-direct {v10}, Ljava/lang/StringBuilder;-><init>()V

    .line 215
    .line 216
    .line 217
    iget-byte v15, v8, Lf/ls0;->Nz:B

    .line 218
    .line 219
    invoke-virtual {v10, v15}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 220
    .line 221
    .line 222
    invoke-virtual {v10, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 223
    .line 224
    .line 225
    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 226
    .line 227
    .line 228
    move-result-object v10

    .line 229
    invoke-static {v9, v10, v12}, Lf/gt0;->aH1(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 230
    .line 231
    .line 232
    move-result-object v9

    .line 233
    iget-short v10, v8, Lf/ls0;->C4:S

    .line 234
    .line 235
    add-int/lit16 v10, v10, -0x1388

    .line 236
    .line 237
    invoke-virtual {v2, v14, v5, v9, v10}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 238
    .line 239
    .line 240
    if-ne v5, v3, :cond_76

    .line 241
    .line 242
    iget-short v8, v8, Lf/ls0;->C4:S

    .line 243
    .line 244
    add-int v8, v8, v16

    .line 245
    .line 246
    invoke-static {v8, v9}, Lf/gt0;->Com5(ILjava/lang/String;)V

    .line 247
    .line 248
    .line 249
    goto/16 :goto_76

    .line 250
    .line 251
    :cond_fa
    const/4 v7, 0x0

    .line 252
    :goto_fb
    sget-object v8, Lf/gt0;->Po0:[[[I

    .line 253
    .line 254
    array-length v9, v8

    .line 255
    if-ge v7, v9, :cond_12c

    .line 256
    .line 257
    aget-object v8, v8, v7

    .line 258
    .line 259
    array-length v9, v8

    .line 260
    const/4 v10, 0x0

    .line 261
    :goto_104
    if-ge v10, v9, :cond_128

    .line 262
    .line 263
    aget-object v11, v8, v10

    .line 264
    .line 265
    aget v14, v11, v4

    .line 266
    .line 267
    aget v15, v11, v13

    .line 268
    .line 269
    invoke-virtual {v0, v14, v5, v15}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 270
    .line 271
    .line 272
    move-result-object v14

    .line 273
    aget v15, v11, v4

    .line 274
    .line 275
    aget v11, v11, v13

    .line 276
    .line 277
    int-to-byte v13, v7

    .line 278
    invoke-static {v1, v13, v6}, Lf/o80;->PE1(BBLf/ub2;)B

    .line 279
    .line 280
    .line 281
    move-result v13

    .line 282
    invoke-static {v13}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    .line 283
    .line 284
    .line 285
    move-result-object v13

    .line 286
    invoke-static {v14, v13, v12}, Lf/gt0;->aH1(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 287
    .line 288
    .line 289
    move-result-object v13

    .line 290
    invoke-virtual {v0, v15, v5, v13, v11}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 291
    .line 292
    .line 293
    add-int/lit8 v10, v10, 0x1

    .line 294
    .line 295
    const/4 v13, 0x1

    .line 296
    goto :goto_104

    .line 297
    :cond_128
    add-int/lit8 v7, v7, 0x1

    .line 298
    .line 299
    const/4 v13, 0x1

    .line 300
    goto :goto_fb

    .line 301
    :cond_12c
    add-int/lit8 v5, v5, 0x1

    .line 302
    .line 303
    goto/16 :goto_17

    .line 304
    .line 305
    :cond_130
    const/4 v0, 0x3

    .line 306
    sget-object v2, Lf/pn2;->SD:Lf/pn2;

    .line 307
    .line 308
    invoke-static {v0, v2}, Lf/gt0;->NP(BLf/pn2;)Lf/ua7;

    .line 309
    .line 310
    .line 311
    move-result-object v0

    .line 312
    const/16 v2, 0x93

    .line 313
    .line 314
    invoke-virtual {v0, v2, v4, v1}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 315
    .line 316
    .line 317
    move-result-object v3

    .line 318
    const/16 v5, 0xfa

    .line 319
    .line 320
    invoke-static {v5}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;

    .line 321
    .line 322
    .line 323
    move-result-object v5

    .line 324
    const-string v13, "[0-9\uff10-\uff19,]{4}"

    .line 325
    .line 326
    invoke-static {v3, v5, v13}, Lf/gt0;->aH1(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 327
    .line 328
    .line 329
    move-result-object v3

    .line 330
    invoke-virtual {v0, v2, v4, v3, v1}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 331
    .line 332
    .line 333
    const/16 v2, 0x9a

    .line 334
    .line 335
    invoke-virtual {v0, v2, v4, v11}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 336
    .line 337
    .line 338
    move-result-object v3

    .line 339
    const/16 v5, 0x13b2

    .line 340
    .line 341
    invoke-static {v3, v5}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 342
    .line 343
    .line 344
    move-result-object v3

    .line 345
    invoke-virtual {v0, v2, v4, v3, v11}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 346
    .line 347
    .line 348
    const/16 v2, 0x1e4

    .line 349
    .line 350
    const/4 v3, 0x1

    .line 351
    invoke-virtual {v0, v2, v4, v3}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 352
    .line 353
    .line 354
    move-result-object v5

    .line 355
    invoke-static {v5, v10}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 356
    .line 357
    .line 358
    move-result-object v5

    .line 359
    invoke-virtual {v0, v2, v4, v5, v3}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 360
    .line 361
    .line 362
    const/16 v2, 0xff

    .line 363
    .line 364
    const/16 v3, 0xa

    .line 365
    .line 366
    invoke-virtual {v0, v2, v4, v3}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 367
    .line 368
    .line 369
    move-result-object v5

    .line 370
    const/16 v11, 0x13e6

    .line 371
    .line 372
    invoke-static {v5, v11}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 373
    .line 374
    .line 375
    move-result-object v5

    .line 376
    invoke-virtual {v0, v2, v4, v5, v3}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 377
    .line 378
    .line 379
    const/16 v2, 0x169

    .line 380
    .line 381
    const/16 v3, 0xdb

    .line 382
    .line 383
    invoke-virtual {v0, v2, v4, v3}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 384
    .line 385
    .line 386
    move-result-object v5

    .line 387
    invoke-static {v5, v9}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 388
    .line 389
    .line 390
    move-result-object v5

    .line 391
    invoke-virtual {v0, v2, v4, v5, v3}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 392
    .line 393
    .line 394
    const/16 v3, 0xdc

    .line 395
    .line 396
    invoke-virtual {v0, v2, v4, v3}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 397
    .line 398
    .line 399
    move-result-object v5

    .line 400
    invoke-static {v5, v8}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 401
    .line 402
    .line 403
    move-result-object v5

    .line 404
    invoke-virtual {v0, v2, v4, v5, v3}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 405
    .line 406
    .line 407
    const/16 v3, 0xdd

    .line 408
    .line 409
    invoke-virtual {v0, v2, v4, v3}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 410
    .line 411
    .line 412
    move-result-object v5

    .line 413
    invoke-static {v5, v7}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 414
    .line 415
    .line 416
    move-result-object v5

    .line 417
    invoke-virtual {v0, v2, v4, v5, v3}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 418
    .line 419
    .line 420
    const/4 v2, 0x0

    .line 421
    :goto_1a4
    sget-object v3, Lf/gt0;->ZT:[[[I

    .line 422
    .line 423
    array-length v5, v3

    .line 424
    if-ge v2, v5, :cond_1d9

    .line 425
    .line 426
    aget-object v3, v3, v2

    .line 427
    .line 428
    array-length v5, v3

    .line 429
    const/4 v11, 0x0

    .line 430
    :goto_1ad
    if-ge v11, v5, :cond_1d4

    .line 431
    .line 432
    aget-object v13, v3, v11

    .line 433
    .line 434
    aget v14, v13, v4

    .line 435
    .line 436
    const/16 v16, 0x1

    .line 437
    .line 438
    aget v15, v13, v16

    .line 439
    .line 440
    invoke-virtual {v0, v14, v4, v15}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 441
    .line 442
    .line 443
    move-result-object v14

    .line 444
    aget v15, v13, v4

    .line 445
    .line 446
    aget v13, v13, v16

    .line 447
    .line 448
    int-to-byte v10, v2

    .line 449
    invoke-static {v1, v10, v6}, Lf/o80;->PE1(BBLf/ub2;)B

    .line 450
    .line 451
    .line 452
    move-result v10

    .line 453
    invoke-static {v10}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    .line 454
    .line 455
    .line 456
    move-result-object v10

    .line 457
    invoke-static {v14, v10, v12}, Lf/gt0;->aH1(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 458
    .line 459
    .line 460
    move-result-object v10

    .line 461
    invoke-virtual {v0, v15, v4, v10, v13}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 462
    .line 463
    .line 464
    add-int/lit8 v11, v11, 0x1

    .line 465
    .line 466
    const/16 v10, 0x13a9

    .line 467
    .line 468
    goto :goto_1ad

    .line 469
    :cond_1d4
    add-int/lit8 v2, v2, 0x1

    .line 470
    .line 471
    const/16 v10, 0x13a9

    .line 472
    .line 473
    goto :goto_1a4

    .line 474
    :cond_1d9
    invoke-static {}, Ljava/text/NumberFormat;->getInstance()Ljava/text/NumberFormat;

    .line 475
    .line 476
    .line 477
    move-result-object v1

    .line 478
    const-wide/16 v2, 0x61a8

    .line 479
    .line 480
    invoke-virtual {v1, v2, v3}, Ljava/text/NumberFormat;->format(J)Ljava/lang/String;

    .line 481
    .line 482
    .line 483
    move-result-object v1

    .line 484
    const v2, 0x1000017

    .line 485
    .line 486
    .line 487
    invoke-static {v2, v1}, Lf/gt0;->RZ(ILjava/lang/String;)Ljava/lang/String;

    .line 488
    .line 489
    .line 490
    move-result-object v1

    .line 491
    const/4 v2, 0x0

    .line 492
    :goto_1eb
    const/4 v3, 0x5

    .line 493
    if-ge v2, v3, :cond_204

    .line 494
    .line 495
    mul-int/lit8 v3, v2, 0x3

    .line 496
    .line 497
    add-int/lit8 v3, v3, 0x14

    .line 498
    .line 499
    const/16 v5, 0x60

    .line 500
    .line 501
    invoke-virtual {v0, v5, v4, v3}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 502
    .line 503
    .line 504
    move-result-object v10

    .line 505
    const-string v11, "\n"

    .line 506
    .line 507
    invoke-static {v10, v11, v1}, Lf/jp3;->sj(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 508
    .line 509
    .line 510
    move-result-object v10

    .line 511
    invoke-virtual {v0, v5, v4, v10, v3}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 512
    .line 513
    .line 514
    add-int/lit8 v2, v2, 0x1

    .line 515
    .line 516
    goto :goto_1eb

    .line 517
    :cond_204
    sget-object v0, Lf/pn2;->SD:Lf/pn2;

    .line 518
    .line 519
    const/4 v1, 0x4

    .line 520
    invoke-static {v1, v0}, Lf/gt0;->NP(BLf/pn2;)Lf/ua7;

    .line 521
    .line 522
    .line 523
    move-result-object v0

    .line 524
    const/16 v2, 0xbf

    .line 525
    .line 526
    const/16 v3, 0xd1

    .line 527
    .line 528
    invoke-virtual {v0, v2, v4, v3}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 529
    .line 530
    .line 531
    move-result-object v5

    .line 532
    invoke-static {v5, v9}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 533
    .line 534
    .line 535
    move-result-object v5

    .line 536
    invoke-virtual {v0, v2, v4, v5, v3}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 537
    .line 538
    .line 539
    const/16 v3, 0xd2

    .line 540
    .line 541
    invoke-virtual {v0, v2, v4, v3}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 542
    .line 543
    .line 544
    move-result-object v5

    .line 545
    invoke-static {v5, v8}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 546
    .line 547
    .line 548
    move-result-object v5

    .line 549
    invoke-virtual {v0, v2, v4, v5, v3}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 550
    .line 551
    .line 552
    const/16 v3, 0xd3

    .line 553
    .line 554
    invoke-virtual {v0, v2, v4, v3}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 555
    .line 556
    .line 557
    move-result-object v5

    .line 558
    invoke-static {v5, v7}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 559
    .line 560
    .line 561
    move-result-object v5

    .line 562
    invoke-virtual {v0, v2, v4, v5, v3}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 563
    .line 564
    .line 565
    const/16 v2, 0x18d

    .line 566
    .line 567
    const/4 v3, 0x1

    .line 568
    invoke-virtual {v0, v2, v4, v3}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 569
    .line 570
    .line 571
    move-result-object v5

    .line 572
    const/16 v7, 0x13a9

    .line 573
    .line 574
    invoke-static {v5, v7}, Lf/gt0;->HT(Ljava/lang/String;S)Ljava/lang/String;

    .line 575
    .line 576
    .line 577
    move-result-object v5

    .line 578
    invoke-virtual {v0, v2, v4, v5, v3}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 579
    .line 580
    .line 581
    const/4 v2, 0x0

    .line 582
    :goto_245
    sget-object v5, Lf/gt0;->Zq1:[[[I

    .line 583
    .line 584
    array-length v7, v5

    .line 585
    if-ge v2, v7, :cond_274

    .line 586
    .line 587
    aget-object v5, v5, v2

    .line 588
    .line 589
    array-length v7, v5

    .line 590
    const/4 v8, 0x0

    .line 591
    :goto_24e
    if-ge v8, v7, :cond_271

    .line 592
    .line 593
    aget-object v9, v5, v8

    .line 594
    .line 595
    aget v10, v9, v4

    .line 596
    .line 597
    aget v11, v9, v3

    .line 598
    .line 599
    invoke-virtual {v0, v10, v4, v11}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 600
    .line 601
    .line 602
    move-result-object v10

    .line 603
    aget v11, v9, v4

    .line 604
    .line 605
    aget v9, v9, v3

    .line 606
    .line 607
    int-to-byte v13, v2

    .line 608
    invoke-static {v1, v13, v6}, Lf/o80;->PE1(BBLf/ub2;)B

    .line 609
    .line 610
    .line 611
    move-result v13

    .line 612
    invoke-static {v13}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    .line 613
    .line 614
    .line 615
    move-result-object v13

    .line 616
    invoke-static {v10, v13, v12}, Lf/gt0;->aH1(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 617
    .line 618
    .line 619
    move-result-object v10

    .line 620
    invoke-virtual {v0, v11, v4, v10, v9}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 621
    .line 622
    .line 623
    add-int/lit8 v8, v8, 0x1

    .line 624
    .line 625
    goto :goto_24e

    .line 626
    :cond_271
    add-int/lit8 v2, v2, 0x1

    .line 627
    .line 628
    goto :goto_245

    .line 629
    :cond_274
    const/16 v1, 0xf6

    .line 630
    .line 631
    const/16 v2, 0xe

    .line 632
    .line 633
    invoke-virtual {v0, v1, v4, v2}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 634
    .line 635
    .line 636
    move-result-object v3

    .line 637
    const-string v5, "60"

    .line 638
    .line 639
    invoke-static {v3, v5, v12}, Lf/gt0;->aH1(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 640
    .line 641
    .line 642
    move-result-object v3

    .line 643
    invoke-virtual {v0, v1, v4, v3, v2}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 644
    .line 645
    .line 646
    const/16 v2, 0xf

    .line 647
    .line 648
    invoke-virtual {v0, v1, v4, v2}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 649
    .line 650
    .line 651
    move-result-object v3

    .line 652
    invoke-static {v3, v5, v12}, Lf/gt0;->aH1(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 653
    .line 654
    .line 655
    move-result-object v3

    .line 656
    invoke-virtual {v0, v1, v4, v3, v2}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 657
    .line 658
    .line 659
    return-void
.end method

.method public static EW0(Ljava/lang/String;[Lf/kg2;)Ljava/lang/String;
    .registers 11

    .line 1
    const-string v0, "{"

    .line 2
    .line 3
    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    .line 4
    .line 5
    .line 6
    move-result v1

    .line 7
    if-nez v1, :cond_a

    .line 8
    .line 9
    goto/16 :goto_a1

    .line 10
    .line 11
    :cond_a
    if-eqz p1, :cond_a1

    .line 12
    .line 13
    array-length v1, p1

    .line 14
    const/4 v2, 0x1

    .line 15
    if-ge v1, v2, :cond_12

    .line 16
    .line 17
    goto/16 :goto_a1

    .line 18
    .line 19
    :cond_12
    array-length v1, p1

    .line 20
    const/4 v3, 0x0

    .line 21
    const/4 v4, 0x0

    .line 22
    :goto_15
    if-ge v4, v1, :cond_49

    .line 23
    .line 24
    aget-object v5, p1, v4

    .line 25
    .line 26
    new-instance v6, Ljava/lang/StringBuilder;

    .line 27
    .line 28
    invoke-direct {v6, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 29
    .line 30
    .line 31
    iget-byte v7, v5, Lf/kg2;->dO:B

    .line 32
    .line 33
    invoke-static {v7}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 34
    .line 35
    .line 36
    move-result-object v7

    .line 37
    new-array v8, v2, [Ljava/lang/Object;

    .line 38
    .line 39
    aput-object v7, v8, v3

    .line 40
    .line 41
    const-string v7, "%1$02X"

    .line 42
    .line 43
    invoke-static {v7, v8}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    .line 44
    .line 45
    .line 46
    move-result-object v7

    .line 47
    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 48
    .line 49
    .line 50
    const-string v7, "}"

    .line 51
    .line 52
    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 53
    .line 54
    .line 55
    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 56
    .line 57
    .line 58
    move-result-object v6

    .line 59
    invoke-virtual {v5}, Lf/kg2;->Rf0()Ljava/lang/String;

    .line 60
    .line 61
    .line 62
    move-result-object v5

    .line 63
    invoke-static {v5}, Ljava/util/regex/Matcher;->quoteReplacement(Ljava/lang/String;)Ljava/lang/String;

    .line 64
    .line 65
    .line 66
    move-result-object v5

    .line 67
    invoke-virtual {p0, v6, v5}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    .line 68
    .line 69
    .line 70
    move-result-object p0

    .line 71
    add-int/lit8 v4, v4, 0x1

    .line 72
    .line 73
    goto :goto_15

    .line 74
    :cond_49
    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    .line 75
    .line 76
    .line 77
    move-result v0

    .line 78
    if-eqz v0, :cond_a1

    .line 79
    .line 80
    const-string v0, "+"

    .line 81
    .line 82
    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    .line 83
    .line 84
    .line 85
    move-result v0

    .line 86
    if-nez v0, :cond_58

    .line 87
    .line 88
    goto :goto_a1

    .line 89
    :cond_58
    const-string v0, "\\{([A-F0-9]{2,4})\\+\\}"

    .line 90
    .line 91
    invoke-static {v0}, Ljava/util/regex/Pattern;->compile(Ljava/lang/String;)Ljava/util/regex/Pattern;

    .line 92
    .line 93
    .line 94
    move-result-object v0

    .line 95
    invoke-virtual {v0, p0}, Ljava/util/regex/Pattern;->matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;

    .line 96
    .line 97
    .line 98
    move-result-object v0

    .line 99
    invoke-virtual {v0}, Ljava/util/regex/Matcher;->find()Z

    .line 100
    .line 101
    .line 102
    move-result v1

    .line 103
    if-eqz v1, :cond_a1

    .line 104
    .line 105
    invoke-virtual {v0, v2}, Ljava/util/regex/Matcher;->group(I)Ljava/lang/String;

    .line 106
    .line 107
    .line 108
    move-result-object v1

    .line 109
    const/16 v2, 0x10

    .line 110
    .line 111
    invoke-static {v1, v2}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;I)I

    .line 112
    .line 113
    .line 114
    move-result v1

    .line 115
    and-int/lit16 v1, v1, 0xff

    .line 116
    .line 117
    int-to-byte v1, v1

    .line 118
    new-instance v2, Ljava/lang/StringBuilder;

    .line 119
    .line 120
    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    .line 121
    .line 122
    .line 123
    array-length v4, p1

    .line 124
    :goto_7b
    if-ge v3, v4, :cond_99

    .line 125
    .line 126
    aget-object v5, p1, v3

    .line 127
    .line 128
    iget-byte v6, v5, Lf/kg2;->dO:B

    .line 129
    .line 130
    if-ge v6, v1, :cond_84

    .line 131
    .line 132
    goto :goto_96

    .line 133
    :cond_84
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->length()I

    .line 134
    .line 135
    .line 136
    move-result v6

    .line 137
    if-lez v6, :cond_8f

    .line 138
    .line 139
    const-string v6, "/"

    .line 140
    .line 141
    invoke-virtual {v2, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 142
    .line 143
    .line 144
    :cond_8f
    invoke-virtual {v5}, Lf/kg2;->Rf0()Ljava/lang/String;

    .line 145
    .line 146
    .line 147
    move-result-object v5

    .line 148
    invoke-virtual {v2, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 149
    .line 150
    .line 151
    :goto_96
    add-int/lit8 v3, v3, 0x1

    .line 152
    .line 153
    goto :goto_7b

    .line 154
    :cond_99
    invoke-virtual {v0}, Ljava/util/regex/Matcher;->group()Ljava/lang/String;

    .line 155
    .line 156
    .line 157
    move-result-object p1

    .line 158
    invoke-virtual {p0, p1, v2}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    .line 159
    .line 160
    .line 161
    move-result-object p0

    .line 162
    :cond_a1
    :goto_a1
    return-object p0
.end method

.method public static HT(Ljava/lang/String;S)Ljava/lang/String;
    .registers 4

    .line 1
    new-instance v0, Ljava/lang/StringBuilder;

    .line 2
    .line 3
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    .line 4
    .line 5
    .line 6
    sget-object v1, Lf/an8;->LU:Lf/an8;

    .line 7
    .line 8
    invoke-virtual {v1, p1}, Lf/an8;->R3(S)Lf/ls0;

    .line 9
    .line 10
    .line 11
    move-result-object p1

    .line 12
    iget p1, p1, Lf/ls0;->FO:I

    .line 13
    .line 14
    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 15
    .line 16
    .line 17
    const-string p1, ""

    .line 18
    .line 19
    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 20
    .line 21
    .line 22
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 23
    .line 24
    .line 25
    move-result-object p1

    .line 26
    const-string v0, "[0-9\uff10-\uff19,]{2,10}(?!\\})"

    .line 27
    .line 28
    invoke-static {p0, p1, v0}, Lf/gt0;->aH1(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 29
    .line 30
    .line 31
    move-result-object p0

    .line 32
    return-object p0
.end method

.method public static IY(I[B[Ljava/lang/String;)Ljava/lang/String;
    .registers 8

    .line 1
    sget-object v0, Lf/gt0;->bJ1:Lf/k89;

    .line 2
    .line 3
    invoke-virtual {v0, p0}, Lf/k89;->get(I)Ljava/lang/Object;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    check-cast v0, Ljava/lang/String;

    .line 8
    .line 9
    if-nez v0, :cond_11

    .line 10
    .line 11
    const-string p1, "STRING_"

    .line 12
    .line 13
    invoke-static {p0, p1}, Lf/cz7;->x01(ILjava/lang/String;)Ljava/lang/String;

    .line 14
    .line 15
    .line 16
    move-result-object p0

    .line 17
    return-object p0

    .line 18
    :cond_11
    array-length p0, p1

    .line 19
    array-length v1, p2

    .line 20
    if-ne p0, v1, :cond_4c

    .line 21
    .line 22
    const/4 p0, 0x0

    .line 23
    const/4 v1, 0x0

    .line 24
    :goto_17
    array-length v2, p1

    .line 25
    if-ge v1, v2, :cond_47

    .line 26
    .line 27
    new-instance v2, Ljava/lang/StringBuilder;

    .line 28
    .line 29
    const-string v3, "\\{"

    .line 30
    .line 31
    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 32
    .line 33
    .line 34
    aget-byte v3, p1, v1

    .line 35
    .line 36
    invoke-static {v3}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 37
    .line 38
    .line 39
    move-result-object v3

    .line 40
    const/4 v4, 0x1

    .line 41
    new-array v4, v4, [Ljava/lang/Object;

    .line 42
    .line 43
    aput-object v3, v4, p0

    .line 44
    .line 45
    const-string v3, "%1$02X"

    .line 46
    .line 47
    invoke-static {v3, v4}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    .line 48
    .line 49
    .line 50
    move-result-object v3

    .line 51
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 52
    .line 53
    .line 54
    const-string v3, "\\}"

    .line 55
    .line 56
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 57
    .line 58
    .line 59
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 60
    .line 61
    .line 62
    move-result-object v2

    .line 63
    aget-object v3, p2, v1

    .line 64
    .line 65
    invoke-virtual {v0, v2, v3}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 66
    .line 67
    .line 68
    move-result-object v0

    .line 69
    add-int/lit8 v1, v1, 0x1

    .line 70
    .line 71
    goto :goto_17

    .line 72
    :cond_47
    invoke-static {v0}, Lf/gt0;->QW1(Ljava/lang/String;)Ljava/lang/String;

    .line 73
    .line 74
    .line 75
    move-result-object p0

    .line 76
    return-object p0

    .line 77
    :cond_4c
    const-string p0, "Mismatching replace keys/values lengths."

    .line 78
    .line 79
    invoke-static {p0}, Lf/qb7;->fn1(Ljava/lang/String;)V

    .line 80
    .line 81
    .line 82
    const/4 p0, 0x0

    .line 83
    return-object p0
.end method

.method public static NP(BLf/pn2;)Lf/ua7;
    .registers 3

    .line 1
    sget-object v0, Lf/gt0;->b91:[[Lf/ua7;

    .line 2
    .line 3
    aget-object p0, v0, p0

    .line 4
    .line 5
    iget-byte p1, p1, Lf/pn2;->rm0:B

    .line 6
    .line 7
    aget-object p0, p0, p1

    .line 8
    .line 9
    return-object p0
.end method

.method public static varargs P31(BLf/pn2;II[Lf/kg2;)Ljava/lang/String;
    .registers 6

    .line 1
    sget-object v0, Lf/gt0;->b91:[[Lf/ua7;

    .line 2
    .line 3
    aget-object p0, v0, p0

    .line 4
    .line 5
    iget-byte p1, p1, Lf/pn2;->rm0:B

    .line 6
    .line 7
    aget-object p0, p0, p1

    .line 8
    .line 9
    const/4 p1, 0x0

    .line 10
    invoke-virtual {p0, p2, p1, p3}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 11
    .line 12
    .line 13
    move-result-object p0

    .line 14
    invoke-static {p0, p4}, Lf/gt0;->EW0(Ljava/lang/String;[Lf/kg2;)Ljava/lang/String;

    .line 15
    .line 16
    .line 17
    move-result-object p0

    .line 18
    invoke-static {p0}, Lf/gt0;->QW1(Ljava/lang/String;)Ljava/lang/String;

    .line 19
    .line 20
    .line 21
    move-result-object p0

    .line 22
    return-object p0
.end method

.method public static QW1(Ljava/lang/String;)Ljava/lang/String;
    .registers 5

    .line 1
    const-string v0, "{"

    .line 2
    .line 3
    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    .line 4
    .line 5
    .line 6
    move-result v0

    .line 7
    if-nez v0, :cond_9

    .line 8
    .line 9
    return-object p0

    .line 10
    :cond_9
    const-string v0, "\\{([A-F0-9]{2,4})\\}"

    .line 11
    .line 12
    invoke-static {v0}, Ljava/util/regex/Pattern;->compile(Ljava/lang/String;)Ljava/util/regex/Pattern;

    .line 13
    .line 14
    .line 15
    move-result-object v0

    .line 16
    invoke-virtual {v0, p0}, Ljava/util/regex/Pattern;->matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;

    .line 17
    .line 18
    .line 19
    move-result-object v0

    .line 20
    :goto_13
    invoke-virtual {v0}, Ljava/util/regex/Matcher;->find()Z

    .line 21
    .line 22
    .line 23
    move-result v1

    .line 24
    if-eqz v1, :cond_3c

    .line 25
    .line 26
    const/4 v1, 0x1

    .line 27
    invoke-virtual {v0, v1}, Ljava/util/regex/Matcher;->group(I)Ljava/lang/String;

    .line 28
    .line 29
    .line 30
    move-result-object v1

    .line 31
    const/16 v2, 0x10

    .line 32
    .line 33
    invoke-static {v1, v2}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;I)I

    .line 34
    .line 35
    .line 36
    move-result v1

    .line 37
    and-int/lit16 v1, v1, 0xff

    .line 38
    .line 39
    int-to-byte v1, v1

    .line 40
    invoke-virtual {v0}, Ljava/util/regex/Matcher;->group()Ljava/lang/String;

    .line 41
    .line 42
    .line 43
    move-result-object v2

    .line 44
    sget-object v3, Lf/x25;->QZ0:Lf/k33;

    .line 45
    .line 46
    invoke-virtual {v3, v1}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 47
    .line 48
    .line 49
    move-result-object v1

    .line 50
    check-cast v1, Ljava/lang/String;

    .line 51
    .line 52
    if-nez v1, :cond_37

    .line 53
    .line 54
    const-string v1, ""

    .line 55
    .line 56
    :cond_37
    invoke-virtual {p0, v2, v1}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    .line 57
    .line 58
    .line 59
    move-result-object p0

    .line 60
    goto :goto_13

    .line 61
    :cond_3c
    return-object p0
.end method

.method public static RF1(Ljava/lang/String;)Lf/kx5;
    .registers 4

    .line 1
    sget-object v0, Lf/gt0;->LPT1:Ljava/util/EnumMap;

    .line 2
    .line 3
    invoke-virtual {v0}, Ljava/util/EnumMap;->entrySet()Ljava/util/Set;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    invoke-interface {v0}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    .line 8
    .line 9
    .line 10
    move-result-object v0

    .line 11
    :cond_a
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    .line 12
    .line 13
    .line 14
    move-result v1

    .line 15
    if-eqz v1, :cond_39

    .line 16
    .line 17
    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 18
    .line 19
    .line 20
    move-result-object v1

    .line 21
    check-cast v1, Ljava/util/Map$Entry;

    .line 22
    .line 23
    invoke-interface {v1}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;

    .line 24
    .line 25
    .line 26
    move-result-object v2

    .line 27
    check-cast v2, Ljava/lang/String;

    .line 28
    .line 29
    invoke-virtual {v2, p0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    .line 30
    .line 31
    .line 32
    move-result v2

    .line 33
    if-nez v2, :cond_32

    .line 34
    .line 35
    invoke-interface {v1}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    .line 36
    .line 37
    .line 38
    move-result-object v2

    .line 39
    check-cast v2, Lf/kx5;

    .line 40
    .line 41
    invoke-virtual {v2}, Ljava/lang/Enum;->name()Ljava/lang/String;

    .line 42
    .line 43
    .line 44
    move-result-object v2

    .line 45
    invoke-virtual {v2, p0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    .line 46
    .line 47
    .line 48
    move-result v2

    .line 49
    if-eqz v2, :cond_a

    .line 50
    .line 51
    :cond_32
    invoke-interface {v1}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;

    .line 52
    .line 53
    .line 54
    move-result-object p0

    .line 55
    check-cast p0, Lf/kx5;

    .line 56
    .line 57
    return-object p0

    .line 58
    :cond_39
    sget-object p0, Lf/kx5;->Tq:Lf/kx5;

    .line 59
    .line 60
    return-object p0
.end method

.method public static RZ(ILjava/lang/String;)Ljava/lang/String;
    .registers 6

    .line 1
    sget-object v0, Lf/gt0;->bJ1:Lf/k89;

    .line 2
    .line 3
    invoke-virtual {v0, p0}, Lf/k89;->get(I)Ljava/lang/Object;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    check-cast v0, Ljava/lang/String;

    .line 8
    .line 9
    if-nez v0, :cond_11

    .line 10
    .line 11
    const-string p1, "STRING_"

    .line 12
    .line 13
    invoke-static {p0, p1}, Lf/cz7;->x01(ILjava/lang/String;)Ljava/lang/String;

    .line 14
    .line 15
    .line 16
    move-result-object p0

    .line 17
    return-object p0

    .line 18
    :cond_11
    new-instance p0, Ljava/lang/StringBuilder;

    .line 19
    .line 20
    const-string v1, "\\{"

    .line 21
    .line 22
    invoke-direct {p0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 23
    .line 24
    .line 25
    const/4 v1, 0x0

    .line 26
    invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 27
    .line 28
    .line 29
    move-result-object v2

    .line 30
    const/4 v3, 0x1

    .line 31
    new-array v3, v3, [Ljava/lang/Object;

    .line 32
    .line 33
    aput-object v2, v3, v1

    .line 34
    .line 35
    const-string v1, "%1$02X"

    .line 36
    .line 37
    invoke-static {v1, v3}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    .line 38
    .line 39
    .line 40
    move-result-object v1

    .line 41
    invoke-virtual {p0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 42
    .line 43
    .line 44
    const-string v1, "\\}"

    .line 45
    .line 46
    invoke-virtual {p0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 47
    .line 48
    .line 49
    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 50
    .line 51
    .line 52
    move-result-object p0

    .line 53
    invoke-static {p1}, Ljava/util/regex/Matcher;->quoteReplacement(Ljava/lang/String;)Ljava/lang/String;

    .line 54
    .line 55
    .line 56
    move-result-object p1

    .line 57
    invoke-virtual {v0, p0, p1}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 58
    .line 59
    .line 60
    move-result-object p0

    .line 61
    invoke-static {p0}, Lf/gt0;->QW1(Ljava/lang/String;)Ljava/lang/String;

    .line 62
    .line 63
    .line 64
    move-result-object p0

    .line 65
    return-object p0
.end method

.method public static Tx(ILjava/lang/String;)Ljava/lang/String;
    .registers 3

    .line 1
    sget-object v0, Lf/gt0;->bJ1:Lf/k89;

    .line 2
    .line 3
    invoke-virtual {v0, p0}, Lf/k89;->get(I)Ljava/lang/Object;

    .line 4
    .line 5
    .line 6
    move-result-object p0

    .line 7
    check-cast p0, Ljava/lang/String;

    .line 8
    .line 9
    if-nez p0, :cond_b

    .line 10
    .line 11
    return-object p1

    .line 12
    :cond_b
    invoke-static {p0}, Lf/gt0;->QW1(Ljava/lang/String;)Ljava/lang/String;

    .line 13
    .line 14
    .line 15
    move-result-object p0

    .line 16
    return-object p0
.end method

.method public static V71(ILjava/lang/String;)V
    .registers 3

    .line 1
    sget-object v0, Lf/gt0;->bJ1:Lf/k89;

    .line 2
    .line 3
    invoke-virtual {v0, p0}, Lf/x44;->rb1(I)I

    .line 4
    .line 5
    .line 6
    move-result p0

    .line 7
    if-gez p0, :cond_10

    .line 8
    .line 9
    iget-object p1, v0, Lf/k89;->Y7:[Ljava/lang/Object;

    .line 10
    .line 11
    neg-int p0, p0

    .line 12
    add-int/lit8 p0, p0, -0x1

    .line 13
    .line 14
    aget-object p0, p1, p0

    .line 15
    .line 16
    return-void

    .line 17
    :cond_10
    invoke-virtual {v0, p0, p1}, Lf/k89;->DE1(ILjava/lang/Object;)Ljava/lang/Object;

    .line 18
    .line 19
    .line 20
    return-void
.end method

.method public static varargs Vf(I[Lf/kg2;)Ljava/lang/String;
    .registers 3

    .line 1
    sget-object v0, Lf/gt0;->bJ1:Lf/k89;

    .line 2
    .line 3
    invoke-virtual {v0, p0}, Lf/k89;->get(I)Ljava/lang/Object;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    check-cast v0, Ljava/lang/String;

    .line 8
    .line 9
    if-nez v0, :cond_11

    .line 10
    .line 11
    const-string p1, "STRING_"

    .line 12
    .line 13
    invoke-static {p0, p1}, Lf/cz7;->x01(ILjava/lang/String;)Ljava/lang/String;

    .line 14
    .line 15
    .line 16
    move-result-object p0

    .line 17
    return-object p0

    .line 18
    :cond_11
    invoke-static {v0, p1}, Lf/gt0;->EW0(Ljava/lang/String;[Lf/kg2;)Ljava/lang/String;

    .line 19
    .line 20
    .line 21
    move-result-object p0

    .line 22
    invoke-static {p0}, Lf/gt0;->QW1(Ljava/lang/String;)Ljava/lang/String;

    .line 23
    .line 24
    .line 25
    move-result-object p0

    .line 26
    return-object p0
.end method

.method public static Vt0(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 7

    .line 1
    new-instance v0, Ljava/lang/StringBuilder;

    .line 2
    .line 3
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    .line 4
    .line 5
    .line 6
    const/4 v1, 0x0

    .line 7
    :goto_6
    invoke-virtual {p0}, Ljava/lang/String;->length()I

    .line 8
    .line 9
    .line 10
    move-result v2

    .line 11
    if-ge v1, v2, :cond_3c

    .line 12
    .line 13
    invoke-virtual {p0, v1}, Ljava/lang/String;->charAt(I)C

    .line 14
    .line 15
    .line 16
    move-result v2

    .line 17
    invoke-static {v2}, Ljava/lang/Character;->isLetter(C)Z

    .line 18
    .line 19
    .line 20
    move-result v3

    .line 21
    if-nez v3, :cond_36

    .line 22
    .line 23
    if-eqz p1, :cond_3c

    .line 24
    .line 25
    invoke-virtual {p1}, Ljava/lang/String;->isEmpty()Z

    .line 26
    .line 27
    .line 28
    move-result v3

    .line 29
    if-nez v3, :cond_3c

    .line 30
    .line 31
    new-instance v3, Ljava/lang/StringBuilder;

    .line 32
    .line 33
    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    .line 34
    .line 35
    .line 36
    invoke-virtual {v3, v2}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    .line 37
    .line 38
    .line 39
    const-string v4, ""

    .line 40
    .line 41
    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 42
    .line 43
    .line 44
    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 45
    .line 46
    .line 47
    move-result-object v3

    .line 48
    invoke-virtual {p1, v3}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    .line 49
    .line 50
    .line 51
    move-result v3

    .line 52
    if-nez v3, :cond_36

    .line 53
    .line 54
    goto :goto_3c

    .line 55
    :cond_36
    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    .line 56
    .line 57
    .line 58
    add-int/lit8 v1, v1, 0x1

    .line 59
    .line 60
    goto :goto_6

    .line 61
    :cond_3c
    :goto_3c
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 62
    .line 63
    .line 64
    move-result-object p0

    .line 65
    return-object p0
.end method

.method public static WP0(Lf/pn2;II)Ljava/lang/String;
    .registers 5

    .line 1
    const/4 v0, 0x2

    .line 2
    sget-object v1, Lf/gt0;->t3:[Ljava/lang/String;

    .line 3
    .line 4
    invoke-static {v0, p0, p1, p2, v1}, Lf/gt0;->tt1(BLf/pn2;II[Ljava/lang/String;)Ljava/lang/String;

    .line 5
    .line 6
    .line 7
    move-result-object p0

    .line 8
    return-object p0
.end method

.method public static Wt0(B)Ljava/lang/String;
    .registers 5

    .line 1
    const/4 v0, 0x3

    .line 2
    sget-object v1, Lf/gt0;->t3:[Ljava/lang/String;

    .line 3
    .line 4
    if-eq p0, v0, :cond_1e

    .line 5
    .line 6
    const/4 v0, 0x4

    .line 7
    if-eq p0, v0, :cond_14

    .line 8
    .line 9
    const p0, 0x4943f

    .line 10
    .line 11
    .line 12
    invoke-static {p0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 13
    .line 14
    .line 15
    move-result-object p0

    .line 16
    invoke-static {p0}, Lf/ay0;->bJ0(Ljava/lang/String;)Ljava/lang/String;

    .line 17
    .line 18
    .line 19
    move-result-object p0

    .line 20
    return-object p0

    .line 21
    :cond_14
    sget-object v0, Lf/pn2;->SD:Lf/pn2;

    .line 22
    .line 23
    const/16 v2, 0x2d9

    .line 24
    .line 25
    const/4 v3, 0x1

    .line 26
    invoke-static {p0, v0, v2, v3, v1}, Lf/gt0;->tt1(BLf/pn2;II[Ljava/lang/String;)Ljava/lang/String;

    .line 27
    .line 28
    .line 29
    move-result-object p0

    .line 30
    return-object p0

    .line 31
    :cond_1e
    sget-object v0, Lf/pn2;->SD:Lf/pn2;

    .line 32
    .line 33
    const/16 v2, 0x185

    .line 34
    .line 35
    const/16 v3, 0x25

    .line 36
    .line 37
    invoke-static {p0, v0, v2, v3, v1}, Lf/gt0;->tt1(BLf/pn2;II[Ljava/lang/String;)Ljava/lang/String;

    .line 38
    .line 39
    .line 40
    move-result-object p0

    .line 41
    return-object p0
.end method

.method public static varargs Ww1(BLf/pn2;II[Ljava/lang/String;)Ljava/lang/String;
    .registers 7

    .line 1
    sget-object v0, Lf/gt0;->b91:[[Lf/ua7;

    .line 2
    .line 3
    aget-object p0, v0, p0

    .line 4
    .line 5
    iget-byte p1, p1, Lf/pn2;->rm0:B

    .line 6
    .line 7
    aget-object p0, p0, p1

    .line 8
    .line 9
    const/4 p1, 0x0

    .line 10
    invoke-virtual {p0, p2, p1, p3}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 11
    .line 12
    .line 13
    move-result-object p0

    .line 14
    const/4 p2, 0x0

    .line 15
    :goto_e
    array-length p3, p4

    .line 16
    if-ge p2, p3, :cond_57

    .line 17
    .line 18
    new-instance p3, Ljava/lang/StringBuilder;

    .line 19
    .line 20
    const-string v0, "\\{"

    .line 21
    .line 22
    invoke-direct {p3, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 23
    .line 24
    .line 25
    invoke-static {p2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 26
    .line 27
    .line 28
    move-result-object v0

    .line 29
    const/4 v1, 0x1

    .line 30
    new-array v1, v1, [Ljava/lang/Object;

    .line 31
    .line 32
    aput-object v0, v1, p1

    .line 33
    .line 34
    const-string v0, "%1$02X"

    .line 35
    .line 36
    invoke-static {v0, v1}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    .line 37
    .line 38
    .line 39
    move-result-object v0

    .line 40
    invoke-virtual {p3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 41
    .line 42
    .line 43
    const-string v0, "\\}"

    .line 44
    .line 45
    invoke-virtual {p3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 46
    .line 47
    .line 48
    invoke-virtual {p3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 49
    .line 50
    .line 51
    move-result-object p3

    .line 52
    new-instance v0, Ljava/lang/StringBuilder;

    .line 53
    .line 54
    const-string v1, "[#ff8a00]"

    .line 55
    .line 56
    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 57
    .line 58
    .line 59
    aget-object v1, p4, p2

    .line 60
    .line 61
    invoke-virtual {v1}, Ljava/lang/String;->trim()Ljava/lang/String;

    .line 62
    .line 63
    .line 64
    move-result-object v1

    .line 65
    invoke-static {v1}, Ljava/util/regex/Matcher;->quoteReplacement(Ljava/lang/String;)Ljava/lang/String;

    .line 66
    .line 67
    .line 68
    move-result-object v1

    .line 69
    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 70
    .line 71
    .line 72
    const-string v1, "[#]"

    .line 73
    .line 74
    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 75
    .line 76
    .line 77
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 78
    .line 79
    .line 80
    move-result-object v0

    .line 81
    invoke-virtual {p0, p3, v0}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 82
    .line 83
    .line 84
    move-result-object p0

    .line 85
    add-int/lit8 p2, p2, 0x1

    .line 86
    .line 87
    goto :goto_e

    .line 88
    :cond_57
    invoke-static {p0}, Lf/gt0;->QW1(Ljava/lang/String;)Ljava/lang/String;

    .line 89
    .line 90
    .line 91
    move-result-object p0

    .line 92
    return-object p0
.end method

.method public static X70(Lf/v67;)V
    .registers 20

    .line 1
    move-object/from16 v0, p0

    .line 2
    .line 3
    const-string v1, " "

    .line 4
    .line 5
    if-nez v0, :cond_7

    .line 6
    .line 7
    return-void

    .line 8
    :cond_7
    sget-object v2, Lf/pn2;->SD:Lf/pn2;

    .line 9
    .line 10
    invoke-virtual {v0, v2}, Lf/sg7;->Nb0(Lf/pn2;)Lf/ua7;

    .line 11
    .line 12
    .line 13
    move-result-object v3

    .line 14
    sget-object v4, Lf/pn2;->lC1:Lf/pn2;

    .line 15
    .line 16
    invoke-virtual {v0, v4}, Lf/sg7;->Nb0(Lf/pn2;)Lf/ua7;

    .line 17
    .line 18
    .line 19
    move-result-object v5

    .line 20
    sget-object v6, Lf/gt0;->b91:[[Lf/ua7;

    .line 21
    .line 22
    const/4 v7, 0x2

    .line 23
    aget-object v8, v6, v7

    .line 24
    .line 25
    iget-byte v9, v2, Lf/pn2;->rm0:B

    .line 26
    .line 27
    aput-object v3, v8, v9

    .line 28
    .line 29
    aget-object v8, v6, v7

    .line 30
    .line 31
    iget-byte v4, v4, Lf/pn2;->rm0:B

    .line 32
    .line 33
    aput-object v5, v8, v4

    .line 34
    .line 35
    const/16 v4, 0x35

    .line 36
    .line 37
    const/4 v5, 0x4

    .line 38
    const/16 v8, 0x59

    .line 39
    .line 40
    const/4 v9, 0x3

    .line 41
    const/16 v10, 0x5c

    .line 42
    .line 43
    const/4 v11, 0x0

    .line 44
    :try_start_2b
    invoke-static {v2, v10, v9}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 45
    .line 46
    .line 47
    move-result-object v13

    .line 48
    const/4 v14, 0x5

    .line 49
    invoke-static {v2, v10, v14}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 50
    .line 51
    .line 52
    move-result-object v14
    :try_end_34
    .catch Ljava/lang/Exception; {:try_start_2b .. :try_end_34} :catch_102

    .line 53
    const/4 v15, 0x6

    .line 54
    const/16 v16, 0x3

    .line 55
    .line 56
    :try_start_37
    invoke-static {v2, v10, v15}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 57
    .line 58
    .line 59
    move-result-object v9
    :try_end_3b
    .catch Ljava/lang/Exception; {:try_start_37 .. :try_end_3b} :catch_fe

    .line 60
    const/16 v17, 0x1

    .line 61
    .line 62
    const/16 v12, 0x2e

    .line 63
    .line 64
    :try_start_3f
    invoke-static {v2, v8, v12}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 65
    .line 66
    .line 67
    move-result-object v12

    .line 68
    invoke-static {v2, v10, v5}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 69
    .line 70
    .line 71
    move-result-object v10

    .line 72
    invoke-virtual {v12, v1}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    .line 73
    .line 74
    .line 75
    move-result v18

    .line 76
    if-eqz v18, :cond_6c

    .line 77
    .line 78
    invoke-virtual {v12, v1}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    .line 79
    .line 80
    .line 81
    move-result-object v1

    .line 82
    invoke-virtual {v0}, Lf/sg7;->OJ0()Ljava/lang/String;

    .line 83
    .line 84
    .line 85
    move-result-object v12

    .line 86
    const-string v8, "it"

    .line 87
    .line 88
    invoke-virtual {v12, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 89
    .line 90
    .line 91
    move-result v8

    .line 92
    if-eqz v8, :cond_5f

    .line 93
    .line 94
    const/4 v8, 0x2

    .line 95
    goto :goto_60

    .line 96
    :cond_5f
    const/4 v8, 0x1

    .line 97
    :goto_60
    array-length v12, v1

    .line 98
    if-lt v8, v12, :cond_6a

    .line 99
    .line 100
    array-length v8, v1

    .line 101
    add-int/lit8 v8, v8, -0x1

    .line 102
    .line 103
    goto :goto_6a

    .line 104
    :catch_67
    move-exception v0

    .line 105
    goto/16 :goto_106

    .line 106
    .line 107
    :cond_6a
    :goto_6a
    aget-object v12, v1, v8

    .line 108
    .line 109
    :cond_6c
    invoke-virtual {v0}, Lf/sg7;->OJ0()Ljava/lang/String;

    .line 110
    .line 111
    .line 112
    move-result-object v1

    .line 113
    const-string v8, "ko"

    .line 114
    .line 115
    invoke-virtual {v1, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 116
    .line 117
    .line 118
    move-result v1

    .line 119
    if-eqz v1, :cond_94

    .line 120
    .line 121
    invoke-virtual {v13, v11, v7}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    .line 122
    .line 123
    .line 124
    move-result-object v13

    .line 125
    invoke-virtual {v14, v11, v7}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    .line 126
    .line 127
    .line 128
    move-result-object v14

    .line 129
    invoke-virtual {v9, v11, v7}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    .line 130
    .line 131
    .line 132
    move-result-object v9

    .line 133
    invoke-virtual {v12}, Ljava/lang/String;->length()I

    .line 134
    .line 135
    .line 136
    move-result v1

    .line 137
    add-int/lit8 v1, v1, -0x3

    .line 138
    .line 139
    invoke-virtual {v12}, Ljava/lang/String;->length()I

    .line 140
    .line 141
    .line 142
    move-result v8

    .line 143
    add-int/lit8 v8, v8, -0x1

    .line 144
    .line 145
    invoke-virtual {v12, v1, v8}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    .line 146
    .line 147
    .line 148
    move-result-object v12

    .line 149
    :cond_94
    invoke-virtual {v0}, Lf/sg7;->OJ0()Ljava/lang/String;

    .line 150
    .line 151
    .line 152
    move-result-object v0

    .line 153
    const-string v1, "ja"

    .line 154
    .line 155
    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 156
    .line 157
    .line 158
    move-result v0

    .line 159
    if-eqz v0, :cond_c8

    .line 160
    .line 161
    invoke-virtual {v13}, Ljava/lang/String;->length()I

    .line 162
    .line 163
    .line 164
    move-result v0

    .line 165
    add-int/lit8 v0, v0, -0x1

    .line 166
    .line 167
    invoke-virtual {v13, v11, v0}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    .line 168
    .line 169
    .line 170
    move-result-object v13

    .line 171
    invoke-virtual {v14}, Ljava/lang/String;->length()I

    .line 172
    .line 173
    .line 174
    move-result v0

    .line 175
    add-int/lit8 v0, v0, -0x1

    .line 176
    .line 177
    invoke-virtual {v14, v11, v0}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    .line 178
    .line 179
    .line 180
    move-result-object v14

    .line 181
    invoke-virtual {v9}, Ljava/lang/String;->length()I

    .line 182
    .line 183
    .line 184
    move-result v0

    .line 185
    add-int/lit8 v0, v0, -0x1

    .line 186
    .line 187
    invoke-virtual {v9, v11, v0}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    .line 188
    .line 189
    .line 190
    move-result-object v9

    .line 191
    const/16 v0, 0xa4

    .line 192
    .line 193
    invoke-static {v2, v4, v0}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 194
    .line 195
    .line 196
    move-result-object v0

    .line 197
    invoke-virtual {v0, v11, v15}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    .line 198
    .line 199
    .line 200
    move-result-object v12

    .line 201
    :cond_c8
    const-string v0, "d\'"

    .line 202
    .line 203
    invoke-virtual {v12, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    .line 204
    .line 205
    .line 206
    move-result v0

    .line 207
    if-eqz v0, :cond_da

    .line 208
    .line 209
    invoke-virtual {v12}, Ljava/lang/String;->length()I

    .line 210
    .line 211
    .line 212
    move-result v0

    .line 213
    if-le v0, v7, :cond_da

    .line 214
    .line 215
    invoke-virtual {v12, v7}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    .line 216
    .line 217
    .line 218
    move-result-object v12

    .line 219
    :cond_da
    const v0, 0x3d090

    .line 220
    .line 221
    .line 222
    invoke-static {v0, v13}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 223
    .line 224
    .line 225
    const v0, 0x3d091

    .line 226
    .line 227
    .line 228
    invoke-static {v0, v14}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 229
    .line 230
    .line 231
    const v0, 0x3d093

    .line 232
    .line 233
    .line 234
    invoke-static {v0, v9}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 235
    .line 236
    .line 237
    const/4 v0, 0x0

    .line 238
    invoke-static {v12, v0}, Lf/gt0;->Vt0(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 239
    .line 240
    .line 241
    move-result-object v0

    .line 242
    const v1, 0x3d092

    .line 243
    .line 244
    .line 245
    invoke-static {v1, v0}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 246
    .line 247
    .line 248
    const v0, 0x3d094

    .line 249
    .line 250
    .line 251
    invoke-static {v0, v10}, Lf/gt0;->V71(ILjava/lang/String;)V
    :try_end_fd
    .catch Ljava/lang/Exception; {:try_start_3f .. :try_end_fd} :catch_67

    .line 252
    .line 253
    .line 254
    goto :goto_109

    .line 255
    :catch_fe
    move-exception v0

    .line 256
    :goto_ff
    const/16 v17, 0x1

    .line 257
    .line 258
    goto :goto_106

    .line 259
    :catch_102
    move-exception v0

    .line 260
    const/16 v16, 0x3

    .line 261
    .line 262
    goto :goto_ff

    .line 263
    :goto_106
    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    .line 264
    .line 265
    .line 266
    :goto_109
    sget-object v0, Lf/pn2;->SD:Lf/pn2;

    .line 267
    .line 268
    const/16 v1, 0x22

    .line 269
    .line 270
    invoke-static {v0, v1, v7}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 271
    .line 272
    .line 273
    move-result-object v2

    .line 274
    invoke-static {v2}, Lf/ay0;->bJ0(Ljava/lang/String;)Ljava/lang/String;

    .line 275
    .line 276
    .line 277
    move-result-object v2

    .line 278
    invoke-static {v11, v2}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 279
    .line 280
    .line 281
    const/4 v2, 0x1

    .line 282
    invoke-static {v0, v1, v2}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 283
    .line 284
    .line 285
    move-result-object v1

    .line 286
    invoke-static {v1}, Lf/ay0;->bJ0(Ljava/lang/String;)Ljava/lang/String;

    .line 287
    .line 288
    .line 289
    move-result-object v1

    .line 290
    invoke-static {v2, v1}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 291
    .line 292
    .line 293
    const/16 v1, 0x11

    .line 294
    .line 295
    const/4 v2, 0x7

    .line 296
    invoke-static {v0, v1, v2}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 297
    .line 298
    .line 299
    move-result-object v1

    .line 300
    invoke-static {v1}, Lf/ay0;->bJ0(Ljava/lang/String;)Ljava/lang/String;

    .line 301
    .line 302
    .line 303
    move-result-object v1

    .line 304
    invoke-static {v7, v1}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 305
    .line 306
    .line 307
    const/16 v1, 0x37

    .line 308
    .line 309
    invoke-static {v0, v1, v7}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 310
    .line 311
    .line 312
    move-result-object v2

    .line 313
    const/4 v8, 0x3

    .line 314
    invoke-static {v8, v2}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 315
    .line 316
    .line 317
    invoke-static {v0, v1, v8}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 318
    .line 319
    .line 320
    move-result-object v1

    .line 321
    invoke-static {v5, v1}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 322
    .line 323
    .line 324
    const/16 v1, 0x5a

    .line 325
    .line 326
    const/16 v2, 0xe

    .line 327
    .line 328
    invoke-static {v0, v1, v2}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 329
    .line 330
    .line 331
    move-result-object v0

    .line 332
    const/16 v1, 0xc

    .line 333
    .line 334
    invoke-static {v1, v0}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 335
    .line 336
    .line 337
    const/16 v0, 0x46

    .line 338
    .line 339
    invoke-virtual {v3, v0}, Lf/ua7;->dy1(I)[Ljava/lang/String;

    .line 340
    .line 341
    .line 342
    move-result-object v0

    .line 343
    const v1, 0x249f0

    .line 344
    .line 345
    .line 346
    invoke-static {v1, v0}, Lf/gt0;->AZ1(I[Ljava/lang/String;)V

    .line 347
    .line 348
    .line 349
    const/16 v0, 0xb6

    .line 350
    .line 351
    invoke-virtual {v3, v0}, Lf/ua7;->dy1(I)[Ljava/lang/String;

    .line 352
    .line 353
    .line 354
    move-result-object v0

    .line 355
    const v1, 0x33450

    .line 356
    .line 357
    .line 358
    invoke-static {v1, v0}, Lf/gt0;->AZ1(I[Ljava/lang/String;)V

    .line 359
    .line 360
    .line 361
    const/16 v0, 0xb7

    .line 362
    .line 363
    invoke-virtual {v3, v0}, Lf/ua7;->dy1(I)[Ljava/lang/String;

    .line 364
    .line 365
    .line 366
    move-result-object v0

    .line 367
    const v1, 0x35b60

    .line 368
    .line 369
    .line 370
    invoke-static {v1, v0}, Lf/gt0;->AZ1(I[Ljava/lang/String;)V

    .line 371
    .line 372
    .line 373
    const/16 v0, 0xca

    .line 374
    .line 375
    invoke-virtual {v3, v0}, Lf/ua7;->dy1(I)[Ljava/lang/String;

    .line 376
    .line 377
    .line 378
    move-result-object v0

    .line 379
    const v1, 0x1d4c0

    .line 380
    .line 381
    .line 382
    invoke-static {v1, v0}, Lf/gt0;->AZ1(I[Ljava/lang/String;)V

    .line 383
    .line 384
    .line 385
    const/16 v0, 0xcb

    .line 386
    .line 387
    invoke-virtual {v3, v0}, Lf/ua7;->dy1(I)[Ljava/lang/String;

    .line 388
    .line 389
    .line 390
    move-result-object v0

    .line 391
    const v1, 0x1adb0

    .line 392
    .line 393
    .line 394
    invoke-static {v1, v0}, Lf/gt0;->AZ1(I[Ljava/lang/String;)V

    .line 395
    .line 396
    .line 397
    const/16 v0, 0xbf

    .line 398
    .line 399
    invoke-virtual {v3, v0}, Lf/ua7;->dy1(I)[Ljava/lang/String;

    .line 400
    .line 401
    .line 402
    move-result-object v0

    .line 403
    const v1, 0x2e720

    .line 404
    .line 405
    .line 406
    invoke-static {v1, v0}, Lf/gt0;->AZ1(I[Ljava/lang/String;)V

    .line 407
    .line 408
    .line 409
    const v0, 0x20f58

    .line 410
    .line 411
    .line 412
    invoke-virtual {v3, v4}, Lf/ua7;->dy1(I)[Ljava/lang/String;

    .line 413
    .line 414
    .line 415
    move-result-object v1

    .line 416
    invoke-static {v0, v1}, Lf/gt0;->AZ1(I[Ljava/lang/String;)V

    .line 417
    .line 418
    .line 419
    const/16 v0, 0x36

    .line 420
    .line 421
    invoke-virtual {v3, v0}, Lf/ua7;->dy1(I)[Ljava/lang/String;

    .line 422
    .line 423
    .line 424
    move-result-object v0

    .line 425
    const v1, 0x3bd08

    .line 426
    .line 427
    .line 428
    invoke-static {v1, v0}, Lf/gt0;->AZ1(I[Ljava/lang/String;)V

    .line 429
    .line 430
    .line 431
    const/16 v0, 0x18

    .line 432
    .line 433
    invoke-virtual {v3, v0}, Lf/ua7;->dy1(I)[Ljava/lang/String;

    .line 434
    .line 435
    .line 436
    move-result-object v0

    .line 437
    const v1, 0x2bf20

    .line 438
    .line 439
    .line 440
    invoke-static {v1, v0}, Lf/gt0;->AZ1(I[Ljava/lang/String;)V

    .line 441
    .line 442
    .line 443
    const v0, 0x22ab0

    .line 444
    .line 445
    .line 446
    const/16 v1, 0x59

    .line 447
    .line 448
    invoke-virtual {v3, v1}, Lf/ua7;->dy1(I)[Ljava/lang/String;

    .line 449
    .line 450
    .line 451
    move-result-object v1

    .line 452
    invoke-static {v0, v1}, Lf/gt0;->AZ1(I[Ljava/lang/String;)V

    .line 453
    .line 454
    .line 455
    sget-object v0, Lf/eb6;->gR0:[Lf/eb6;

    .line 456
    .line 457
    array-length v1, v0

    .line 458
    const/4 v2, 0x0

    .line 459
    :goto_1ca
    if-ge v2, v1, :cond_1f1

    .line 460
    .line 461
    aget-object v4, v0, v2

    .line 462
    .line 463
    sget-object v5, Lf/eb6;->nw0:Lf/eb6;

    .line 464
    .line 465
    const v8, 0x38270

    .line 466
    .line 467
    .line 468
    if-ne v4, v5, :cond_1de

    .line 469
    .line 470
    iget-byte v4, v4, Lf/eb6;->qx:B

    .line 471
    .line 472
    add-int/2addr v4, v8

    .line 473
    const-string v5, "???"

    .line 474
    .line 475
    invoke-static {v4, v5}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 476
    .line 477
    .line 478
    goto :goto_1ee

    .line 479
    :cond_1de
    iget-byte v5, v4, Lf/eb6;->qx:B

    .line 480
    .line 481
    add-int/2addr v5, v8

    .line 482
    sget-object v8, Lf/pn2;->SD:Lf/pn2;

    .line 483
    .line 484
    const/16 v9, 0xc7

    .line 485
    .line 486
    iget-byte v4, v4, Lf/eb6;->oE0:B

    .line 487
    .line 488
    invoke-static {v8, v9, v4}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 489
    .line 490
    .line 491
    move-result-object v4

    .line 492
    invoke-static {v5, v4}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 493
    .line 494
    .line 495
    :goto_1ee
    add-int/lit8 v2, v2, 0x1

    .line 496
    .line 497
    goto :goto_1ca

    .line 498
    :cond_1f1
    sget-object v0, Lf/rb8;->ro0:Lf/rb8;

    .line 499
    .line 500
    const v0, 0x3be50

    .line 501
    .line 502
    .line 503
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 504
    .line 505
    .line 506
    move-result-object v0

    .line 507
    const-string v1, "??"

    .line 508
    .line 509
    const-string v2, "[0-9\uff10-\uff19,]{2,10}(?!\\})"

    .line 510
    .line 511
    invoke-static {v0, v1, v2}, Lf/gt0;->aH1(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 512
    .line 513
    .line 514
    move-result-object v0

    .line 515
    const/16 v1, 0x6da

    .line 516
    .line 517
    invoke-static {v1, v0}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 518
    .line 519
    .line 520
    sget-object v0, Lf/pn2;->SD:Lf/pn2;

    .line 521
    .line 522
    aget-object v1, v6, v7

    .line 523
    .line 524
    iget-byte v2, v0, Lf/pn2;->rm0:B

    .line 525
    .line 526
    iget-byte v4, v0, Lf/pn2;->rm0:B

    .line 527
    .line 528
    aget-object v1, v1, v2

    .line 529
    .line 530
    const/16 v2, 0x9d

    .line 531
    .line 532
    const/16 v5, 0x29

    .line 533
    .line 534
    invoke-virtual {v1, v2, v11, v5}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 535
    .line 536
    .line 537
    move-result-object v1

    .line 538
    const v5, 0x31128

    .line 539
    .line 540
    .line 541
    invoke-static {v5, v1}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 542
    .line 543
    .line 544
    aget-object v1, v6, v7

    .line 545
    .line 546
    aget-object v1, v1, v4

    .line 547
    .line 548
    const/16 v5, 0x2a

    .line 549
    .line 550
    invoke-virtual {v1, v2, v11, v5}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 551
    .line 552
    .line 553
    move-result-object v1

    .line 554
    const v5, 0x31129

    .line 555
    .line 556
    .line 557
    invoke-static {v5, v1}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 558
    .line 559
    .line 560
    aget-object v1, v6, v7

    .line 561
    .line 562
    aget-object v1, v1, v4

    .line 563
    .line 564
    const/16 v4, 0x2b

    .line 565
    .line 566
    invoke-virtual {v1, v2, v11, v4}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 567
    .line 568
    .line 569
    move-result-object v1

    .line 570
    const v2, 0x3112f

    .line 571
    .line 572
    .line 573
    invoke-static {v2, v1}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 574
    .line 575
    .line 576
    const/16 v1, 0xf

    .line 577
    .line 578
    const/16 v2, 0x52

    .line 579
    .line 580
    invoke-static {v0, v1, v2}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 581
    .line 582
    .line 583
    move-result-object v0

    .line 584
    const v1, 0x30e34

    .line 585
    .line 586
    .line 587
    invoke-static {v1, v0}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 588
    .line 589
    .line 590
    const/16 v0, 0x23

    .line 591
    .line 592
    invoke-virtual {v3, v0}, Lf/ua7;->dy1(I)[Ljava/lang/String;

    .line 593
    .line 594
    .line 595
    move-result-object v0

    .line 596
    const v1, 0x3d540

    .line 597
    .line 598
    .line 599
    invoke-static {v1, v0}, Lf/gt0;->AZ1(I[Ljava/lang/String;)V

    .line 600
    .line 601
    .line 602
    return-void
.end method

.method public static aF0(II)Ljava/lang/String;
    .registers 5

    .line 1
    sget-object v0, Lf/pn2;->SD:Lf/pn2;

    .line 2
    .line 3
    sget-object v1, Lf/gt0;->t3:[Ljava/lang/String;

    .line 4
    .line 5
    const/4 v2, 0x4

    .line 6
    invoke-static {v2, v0, p0, p1, v1}, Lf/gt0;->tt1(BLf/pn2;II[Ljava/lang/String;)Ljava/lang/String;

    .line 7
    .line 8
    .line 9
    move-result-object p0

    .line 10
    return-object p0
.end method

.method public static aH1(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 9

    .line 1
    invoke-virtual {p0}, Ljava/lang/String;->toCharArray()[C

    .line 2
    .line 3
    .line 4
    move-result-object v0

    .line 5
    array-length v1, v0

    .line 6
    const/4 v2, 0x0

    .line 7
    const/4 v3, 0x0

    .line 8
    :goto_7
    if-ge v3, v1, :cond_30

    .line 9
    .line 10
    aget-char v4, v0, v3

    .line 11
    .line 12
    invoke-static {v4}, Ljava/lang/Character$UnicodeBlock;->of(C)Ljava/lang/Character$UnicodeBlock;

    .line 13
    .line 14
    .line 15
    move-result-object v4

    .line 16
    sget-object v5, Ljava/lang/Character$UnicodeBlock;->HALFWIDTH_AND_FULLWIDTH_FORMS:Ljava/lang/Character$UnicodeBlock;

    .line 17
    .line 18
    if-ne v4, v5, :cond_2d

    .line 19
    .line 20
    invoke-virtual {p1}, Ljava/lang/String;->toCharArray()[C

    .line 21
    .line 22
    .line 23
    move-result-object p1

    .line 24
    :goto_17
    array-length v0, p1

    .line 25
    if-ge v2, v0, :cond_26

    .line 26
    .line 27
    aget-char v0, p1, v2

    .line 28
    .line 29
    const v1, 0xfee0

    .line 30
    .line 31
    .line 32
    add-int/2addr v0, v1

    .line 33
    int-to-char v0, v0

    .line 34
    aput-char v0, p1, v2

    .line 35
    .line 36
    add-int/lit8 v2, v2, 0x1

    .line 37
    .line 38
    goto :goto_17

    .line 39
    :cond_26
    new-instance v0, Ljava/lang/String;

    .line 40
    .line 41
    invoke-direct {v0, p1}, Ljava/lang/String;-><init>([C)V

    .line 42
    .line 43
    .line 44
    move-object p1, v0

    .line 45
    goto :goto_30

    .line 46
    :cond_2d
    add-int/lit8 v3, v3, 0x1

    .line 47
    .line 48
    goto :goto_7

    .line 49
    :cond_30
    :goto_30
    invoke-virtual {p0, p2, p1}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 50
    .line 51
    .line 52
    move-result-object p0

    .line 53
    return-object p0
.end method

.method public static bC1(IILf/th7;)V
    .registers 5

    .line 1
    const/4 v0, -0x1

    .line 2
    const/4 v1, 0x0

    .line 3
    invoke-static {p2, p0, p1, v0, v1}, Lf/gt0;->kN1(Lf/th7;IIIZ)V

    .line 4
    .line 5
    .line 6
    return-void
.end method

.method public static dT0(B)Ljava/lang/String;
    .registers 1

    .line 1
    add-int/lit8 p0, p0, 0x5a

    .line 2
    .line 3
    invoke-static {p0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 4
    .line 5
    .line 6
    move-result-object p0

    .line 7
    return-object p0
.end method

.method public static el()V
    .registers 10

    .line 1
    const v0, 0x3be50

    .line 2
    .line 3
    .line 4
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 5
    .line 6
    .line 7
    move-result-object v0

    .line 8
    const-string v1, ""

    .line 9
    .line 10
    const-string v2, "[0-9\uff10-\uff19,]{2,10}(?!\\})"

    .line 11
    .line 12
    invoke-static {v0, v1, v2}, Lf/gt0;->aH1(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 13
    .line 14
    .line 15
    move-result-object v0

    .line 16
    const v3, 0x3beac

    .line 17
    .line 18
    .line 19
    invoke-static {v3}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 20
    .line 21
    .line 22
    move-result-object v3

    .line 23
    invoke-static {v3, v1, v2}, Lf/gt0;->aH1(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 24
    .line 25
    .line 26
    move-result-object v1

    .line 27
    const v2, 0xde2b0

    .line 28
    .line 29
    .line 30
    invoke-static {v2, v1}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 31
    .line 32
    .line 33
    invoke-static {v2}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 34
    .line 35
    .line 36
    move-result-object v1

    .line 37
    sget-object v2, Lf/an8;->LU:Lf/an8;

    .line 38
    .line 39
    iget-object v2, v2, Lf/an8;->lO:Ljava/util/TreeMap;

    .line 40
    .line 41
    invoke-virtual {v2}, Ljava/util/TreeMap;->values()Ljava/util/Collection;

    .line 42
    .line 43
    .line 44
    move-result-object v2

    .line 45
    invoke-interface {v2}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    .line 46
    .line 47
    .line 48
    move-result-object v2

    .line 49
    :cond_30
    :goto_30
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    .line 50
    .line 51
    .line 52
    move-result v3

    .line 53
    const v4, 0x1d4c0

    .line 54
    .line 55
    .line 56
    if-eqz v3, :cond_95

    .line 57
    .line 58
    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 59
    .line 60
    .line 61
    move-result-object v3

    .line 62
    check-cast v3, Lf/ls0;

    .line 63
    .line 64
    iget-short v5, v3, Lf/ls0;->lj:S

    .line 65
    .line 66
    if-lez v5, :cond_30

    .line 67
    .line 68
    iget-short v5, v3, Lf/ls0;->C4:S

    .line 69
    .line 70
    const v6, 0xdbba0

    .line 71
    .line 72
    .line 73
    add-int/2addr v6, v5

    .line 74
    const v7, 0xebba0

    .line 75
    .line 76
    .line 77
    add-int/2addr v5, v7

    .line 78
    new-instance v7, Ljava/lang/StringBuilder;

    .line 79
    .line 80
    invoke-direct {v7}, Ljava/lang/StringBuilder;-><init>()V

    .line 81
    .line 82
    .line 83
    invoke-virtual {v3}, Lf/ls0;->jU0()Z

    .line 84
    .line 85
    .line 86
    move-result v8

    .line 87
    if-eqz v8, :cond_5a

    .line 88
    .line 89
    move-object v8, v1

    .line 90
    goto :goto_5b

    .line 91
    :cond_5a
    move-object v8, v0

    .line 92
    :goto_5b
    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 93
    .line 94
    .line 95
    const-string v8, " - "

    .line 96
    .line 97
    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 98
    .line 99
    .line 100
    iget-short v8, v3, Lf/ls0;->lj:S

    .line 101
    .line 102
    if-lez v8, :cond_7a

    .line 103
    .line 104
    invoke-static {}, Lf/k92;->dh0()Lf/k92;

    .line 105
    .line 106
    .line 107
    move-result-object v8

    .line 108
    iget-short v9, v3, Lf/ls0;->lj:S

    .line 109
    .line 110
    invoke-virtual {v8, v9}, Lf/k92;->BW1(S)Lf/hu6;

    .line 111
    .line 112
    .line 113
    move-result-object v8

    .line 114
    if-eqz v8, :cond_7a

    .line 115
    .line 116
    iget v8, v8, Lf/hu6;->bl1:I

    .line 117
    .line 118
    invoke-static {v8}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 119
    .line 120
    .line 121
    move-result-object v8

    .line 122
    goto :goto_7c

    .line 123
    :cond_7a
    const-string v8, "--"

    .line 124
    .line 125
    :goto_7c
    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 126
    .line 127
    .line 128
    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 129
    .line 130
    .line 131
    move-result-object v7

    .line 132
    invoke-static {v6, v7}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 133
    .line 134
    .line 135
    iget-short v7, v3, Lf/ls0;->lj:S

    .line 136
    .line 137
    add-int/2addr v7, v4

    .line 138
    invoke-static {v7}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 139
    .line 140
    .line 141
    move-result-object v4

    .line 142
    invoke-static {v5, v4}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 143
    .line 144
    .line 145
    iput v6, v3, Lf/ls0;->FU:I

    .line 146
    .line 147
    iput v5, v3, Lf/ls0;->Dt:I

    .line 148
    .line 149
    goto :goto_30

    .line 150
    :cond_95
    const/4 v0, 0x2

    .line 151
    sget-object v1, Lf/pn2;->SD:Lf/pn2;

    .line 152
    .line 153
    invoke-static {v0, v1}, Lf/gt0;->NP(BLf/pn2;)Lf/ua7;

    .line 154
    .line 155
    .line 156
    move-result-object v0

    .line 157
    const/16 v1, 0xf4

    .line 158
    .line 159
    invoke-virtual {v0, v1}, Lf/ua7;->dy1(I)[Ljava/lang/String;

    .line 160
    .line 161
    .line 162
    move-result-object v0

    .line 163
    const/4 v1, 0x0

    .line 164
    const/4 v2, 0x0

    .line 165
    :goto_a4
    array-length v3, v0

    .line 166
    if-ge v2, v3, :cond_b3

    .line 167
    .line 168
    aget-object v3, v0, v2

    .line 169
    .line 170
    const v5, 0x25d78

    .line 171
    .line 172
    .line 173
    add-int/2addr v5, v2

    .line 174
    invoke-static {v5, v3}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 175
    .line 176
    .line 177
    add-int/lit8 v2, v2, 0x1

    .line 178
    .line 179
    goto :goto_a4

    .line 180
    :cond_b3
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 181
    .line 182
    .line 183
    move-result-object v0

    .line 184
    iget-object v0, v0, Lf/y91;->Uv:Ljava/util/HashMap;

    .line 185
    .line 186
    invoke-virtual {v0}, Ljava/util/HashMap;->values()Ljava/util/Collection;

    .line 187
    .line 188
    .line 189
    move-result-object v0

    .line 190
    invoke-interface {v0}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    .line 191
    .line 192
    .line 193
    move-result-object v0

    .line 194
    :goto_c1
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    .line 195
    .line 196
    .line 197
    move-result v2

    .line 198
    const v3, 0x249f0

    .line 199
    .line 200
    .line 201
    if-eqz v2, :cond_f8

    .line 202
    .line 203
    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 204
    .line 205
    .line 206
    move-result-object v2

    .line 207
    check-cast v2, Lf/zp3;

    .line 208
    .line 209
    invoke-virtual {v2}, Lf/zp3;->nd0()Z

    .line 210
    .line 211
    .line 212
    move-result v5

    .line 213
    if-nez v5, :cond_d7

    .line 214
    .line 215
    goto :goto_c1

    .line 216
    :cond_d7
    iget-object v5, v2, Lf/zp3;->oF:Lf/zp3;

    .line 217
    .line 218
    if-eqz v5, :cond_dc

    .line 219
    .line 220
    goto :goto_dd

    .line 221
    :cond_dc
    move-object v5, v2

    .line 222
    :goto_dd
    iget-short v6, v2, Lf/zp3;->Kj1:S

    .line 223
    .line 224
    add-int/2addr v6, v3

    .line 225
    invoke-virtual {v5, v1}, Lf/zp3;->xt(Z)Ljava/lang/String;

    .line 226
    .line 227
    .line 228
    move-result-object v3

    .line 229
    iget-byte v5, v2, Lf/zp3;->x01:B

    .line 230
    .line 231
    invoke-virtual {v2, v5}, Lf/zp3;->Uq(B)Ljava/lang/String;

    .line 232
    .line 233
    .line 234
    move-result-object v2

    .line 235
    filled-new-array {v3, v2}, [Ljava/lang/String;

    .line 236
    .line 237
    .line 238
    move-result-object v2

    .line 239
    const/16 v3, 0x6e4

    .line 240
    .line 241
    invoke-static {v3, v2}, Lf/gt0;->fQ0(I[Ljava/lang/String;)Ljava/lang/String;

    .line 242
    .line 243
    .line 244
    move-result-object v2

    .line 245
    invoke-static {v6, v2}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 246
    .line 247
    .line 248
    goto :goto_c1

    .line 249
    :cond_f8
    sget-object v0, Lf/ux4;->vT0:[[S

    .line 250
    .line 251
    const/4 v2, 0x0

    .line 252
    :goto_fb
    const/16 v5, 0x12

    .line 253
    .line 254
    if-ge v2, v5, :cond_112

    .line 255
    .line 256
    aget-object v5, v0, v2

    .line 257
    .line 258
    const/4 v6, 0x1

    .line 259
    aget-short v6, v5, v6

    .line 260
    .line 261
    add-int/2addr v6, v3

    .line 262
    aget-short v5, v5, v1

    .line 263
    .line 264
    add-int/2addr v5, v3

    .line 265
    invoke-static {v5}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 266
    .line 267
    .line 268
    move-result-object v5

    .line 269
    invoke-static {v6, v5}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 270
    .line 271
    .line 272
    add-int/lit8 v2, v2, 0x1

    .line 273
    .line 274
    goto :goto_fb

    .line 275
    :cond_112
    const/4 v0, 0x0

    .line 276
    :goto_113
    sget-object v2, Lf/y17;->eB1:[Lf/y17;

    .line 277
    .line 278
    array-length v3, v2

    .line 279
    if-ge v0, v3, :cond_13b

    .line 280
    .line 281
    const v3, 0xe30d0

    .line 282
    .line 283
    .line 284
    add-int/2addr v3, v0

    .line 285
    aget-object v2, v2, v0

    .line 286
    .line 287
    invoke-virtual {v2}, Lf/y17;->toString()Ljava/lang/String;

    .line 288
    .line 289
    .line 290
    move-result-object v2

    .line 291
    const v5, 0x18d49

    .line 292
    .line 293
    .line 294
    invoke-static {v5, v2}, Lf/gt0;->RZ(ILjava/lang/String;)Ljava/lang/String;

    .line 295
    .line 296
    .line 297
    move-result-object v2

    .line 298
    invoke-static {v3, v2}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 299
    .line 300
    .line 301
    sget-object v2, Lf/an8;->LU:Lf/an8;

    .line 302
    .line 303
    const/16 v5, 0x5a7

    .line 304
    .line 305
    add-int/2addr v5, v0

    .line 306
    int-to-short v5, v5

    .line 307
    invoke-virtual {v2, v5}, Lf/an8;->R3(S)Lf/ls0;

    .line 308
    .line 309
    .line 310
    move-result-object v2

    .line 311
    iput v3, v2, Lf/ls0;->FU:I

    .line 312
    .line 313
    add-int/lit8 v0, v0, 0x1

    .line 314
    .line 315
    goto :goto_113

    .line 316
    :cond_13b
    invoke-static {}, Lf/k92;->dh0()Lf/k92;

    .line 317
    .line 318
    .line 319
    move-result-object v0

    .line 320
    invoke-virtual {v0}, Lf/k92;->pa0()Lf/u48;

    .line 321
    .line 322
    .line 323
    move-result-object v0

    .line 324
    invoke-virtual {v0}, Lf/u48;->iterator()Ljava/util/Iterator;

    .line 325
    .line 326
    .line 327
    move-result-object v0

    .line 328
    :goto_147
    move-object v2, v0

    .line 329
    check-cast v2, Lf/rk3;

    .line 330
    .line 331
    invoke-virtual {v2}, Lf/rk3;->hasNext()Z

    .line 332
    .line 333
    .line 334
    move-result v2

    .line 335
    if-eqz v2, :cond_18f

    .line 336
    .line 337
    move-object v2, v0

    .line 338
    check-cast v2, Lf/xx1;

    .line 339
    .line 340
    invoke-virtual {v2}, Lf/xx1;->next()Ljava/lang/Object;

    .line 341
    .line 342
    .line 343
    move-result-object v2

    .line 344
    check-cast v2, Lf/hu6;

    .line 345
    .line 346
    iget-short v2, v2, Lf/hu6;->m21:S

    .line 347
    .line 348
    const/16 v3, 0xbb8

    .line 349
    .line 350
    if-ge v2, v3, :cond_160

    .line 351
    .line 352
    goto :goto_147

    .line 353
    :cond_160
    const v3, 0x1adb0

    .line 354
    .line 355
    .line 356
    add-int/2addr v3, v2

    .line 357
    new-instance v5, Ljava/lang/StringBuilder;

    .line 358
    .line 359
    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    .line 360
    .line 361
    .line 362
    const v6, -0x1a1f8

    .line 363
    .line 364
    .line 365
    sub-int v6, v2, v6

    .line 366
    .line 367
    invoke-static {v6}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 368
    .line 369
    .line 370
    move-result-object v6

    .line 371
    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 372
    .line 373
    .line 374
    const-string v6, "\u2606"

    .line 375
    .line 376
    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 377
    .line 378
    .line 379
    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 380
    .line 381
    .line 382
    move-result-object v5

    .line 383
    invoke-static {v3, v5}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 384
    .line 385
    .line 386
    add-int v3, v2, v4

    .line 387
    .line 388
    const v5, -0x1c908

    .line 389
    .line 390
    .line 391
    sub-int/2addr v2, v5

    .line 392
    invoke-static {v2}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 393
    .line 394
    .line 395
    move-result-object v2

    .line 396
    invoke-static {v3, v2}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 397
    .line 398
    .line 399
    goto :goto_147

    .line 400
    :cond_18f
    new-instance v0, Ljava/lang/StringBuilder;

    .line 401
    .line 402
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    .line 403
    .line 404
    .line 405
    sget-object v2, Lf/c89;->Fs0:[Lf/c89;

    .line 406
    .line 407
    array-length v3, v2

    .line 408
    const/4 v4, 0x0

    .line 409
    :goto_198
    if-ge v4, v3, :cond_1c3

    .line 410
    .line 411
    aget-object v5, v2, v4

    .line 412
    .line 413
    iget-byte v6, v5, Lf/c89;->N50:B

    .line 414
    .line 415
    const/16 v7, 0x2bc0

    .line 416
    .line 417
    add-int/2addr v7, v6

    .line 418
    invoke-static {v7}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 419
    .line 420
    .line 421
    move-result-object v6

    .line 422
    invoke-virtual {v0, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 423
    .line 424
    .line 425
    const/16 v6, 0x2af7

    .line 426
    .line 427
    invoke-static {v6}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 428
    .line 429
    .line 430
    move-result-object v6

    .line 431
    invoke-virtual {v0, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 432
    .line 433
    .line 434
    iget-byte v5, v5, Lf/c89;->N50:B

    .line 435
    .line 436
    const/16 v6, 0x2c88

    .line 437
    .line 438
    add-int/2addr v6, v5

    .line 439
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 440
    .line 441
    .line 442
    move-result-object v5

    .line 443
    invoke-static {v6, v5}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 444
    .line 445
    .line 446
    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->setLength(I)V

    .line 447
    .line 448
    .line 449
    add-int/lit8 v4, v4, 0x1

    .line 450
    .line 451
    goto :goto_198

    .line 452
    :cond_1c3
    return-void
.end method

.method public static f30(I)V
    .registers 8

    .line 1
    const/4 v0, 0x0

    .line 2
    invoke-static {p0, v0}, Lf/gt0;->aF0(II)Ljava/lang/String;

    .line 3
    .line 4
    .line 5
    move-result-object v1

    .line 6
    const/4 v2, 0x4

    .line 7
    sget-object v3, Lf/pn2;->SD:Lf/pn2;

    .line 8
    .line 9
    invoke-static {v2, v3}, Lf/gt0;->NP(BLf/pn2;)Lf/ua7;

    .line 10
    .line 11
    .line 12
    move-result-object v2

    .line 13
    :goto_c
    const/4 v3, 0x2

    .line 14
    if-ge v0, v3, :cond_30

    .line 15
    .line 16
    invoke-virtual {v2, p0, v0}, Lf/ua7;->Py0(II)I

    .line 17
    .line 18
    .line 19
    move-result v3

    .line 20
    const/4 v4, 0x1

    .line 21
    :goto_14
    if-ge v4, v3, :cond_2d

    .line 22
    .line 23
    invoke-virtual {v2, p0, v0, v4}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 24
    .line 25
    .line 26
    move-result-object v5

    .line 27
    invoke-virtual {v5, v1}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    .line 28
    .line 29
    .line 30
    move-result v6

    .line 31
    if-eqz v6, :cond_21

    .line 32
    .line 33
    goto :goto_2a

    .line 34
    :cond_21
    const-string v6, ": "

    .line 35
    .line 36
    invoke-static {v1, v6, v5}, Lf/jp3;->sj(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 37
    .line 38
    .line 39
    move-result-object v5

    .line 40
    invoke-virtual {v2, p0, v0, v5, v4}, Lf/ua7;->ky1(IILjava/lang/String;I)V

    .line 41
    .line 42
    .line 43
    :goto_2a
    add-int/lit8 v4, v4, 0x1

    .line 44
    .line 45
    goto :goto_14

    .line 46
    :cond_2d
    add-int/lit8 v0, v0, 0x1

    .line 47
    .line 48
    goto :goto_c

    .line 49
    :cond_30
    return-void
.end method

.method public static varargs fN(I[Ljava/lang/String;)Ljava/lang/String;
    .registers 7

    .line 1
    sget-object v0, Lf/gt0;->bJ1:Lf/k89;

    .line 2
    .line 3
    invoke-virtual {v0, p0}, Lf/k89;->get(I)Ljava/lang/Object;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    check-cast v0, Ljava/lang/String;

    .line 8
    .line 9
    if-nez v0, :cond_11

    .line 10
    .line 11
    const-string p1, "STRING_"

    .line 12
    .line 13
    invoke-static {p0, p1}, Lf/cz7;->x01(ILjava/lang/String;)Ljava/lang/String;

    .line 14
    .line 15
    .line 16
    move-result-object p0

    .line 17
    return-object p0

    .line 18
    :cond_11
    const/4 p0, 0x0

    .line 19
    const/4 v1, 0x0

    .line 20
    :goto_13
    array-length v2, p1

    .line 21
    if-ge v1, v2, :cond_5c

    .line 22
    .line 23
    new-instance v2, Ljava/lang/StringBuilder;

    .line 24
    .line 25
    const-string v3, "\\{"

    .line 26
    .line 27
    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 28
    .line 29
    .line 30
    invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 31
    .line 32
    .line 33
    move-result-object v3

    .line 34
    const/4 v4, 0x1

    .line 35
    new-array v4, v4, [Ljava/lang/Object;

    .line 36
    .line 37
    aput-object v3, v4, p0

    .line 38
    .line 39
    const-string v3, "%1$02X"

    .line 40
    .line 41
    invoke-static {v3, v4}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    .line 42
    .line 43
    .line 44
    move-result-object v3

    .line 45
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 46
    .line 47
    .line 48
    const-string v3, "\\}"

    .line 49
    .line 50
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 51
    .line 52
    .line 53
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 54
    .line 55
    .line 56
    move-result-object v2

    .line 57
    new-instance v3, Ljava/lang/StringBuilder;

    .line 58
    .line 59
    const-string v4, "[#ff8a00]"

    .line 60
    .line 61
    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 62
    .line 63
    .line 64
    aget-object v4, p1, v1

    .line 65
    .line 66
    invoke-virtual {v4}, Ljava/lang/String;->trim()Ljava/lang/String;

    .line 67
    .line 68
    .line 69
    move-result-object v4

    .line 70
    invoke-static {v4}, Ljava/util/regex/Matcher;->quoteReplacement(Ljava/lang/String;)Ljava/lang/String;

    .line 71
    .line 72
    .line 73
    move-result-object v4

    .line 74
    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 75
    .line 76
    .line 77
    const-string v4, "[#]"

    .line 78
    .line 79
    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 80
    .line 81
    .line 82
    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 83
    .line 84
    .line 85
    move-result-object v3

    .line 86
    invoke-virtual {v0, v2, v3}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 87
    .line 88
    .line 89
    move-result-object v0

    .line 90
    add-int/lit8 v1, v1, 0x1

    .line 91
    .line 92
    goto :goto_13

    .line 93
    :cond_5c
    invoke-static {v0}, Lf/gt0;->QW1(Ljava/lang/String;)Ljava/lang/String;

    .line 94
    .line 95
    .line 96
    move-result-object p0

    .line 97
    return-object p0
.end method

.method public static varargs fQ0(I[Ljava/lang/String;)Ljava/lang/String;
    .registers 7

    .line 1
    sget-object v0, Lf/gt0;->bJ1:Lf/k89;

    .line 2
    .line 3
    invoke-virtual {v0, p0}, Lf/k89;->get(I)Ljava/lang/Object;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    check-cast v0, Ljava/lang/String;

    .line 8
    .line 9
    if-nez v0, :cond_11

    .line 10
    .line 11
    const-string p1, "STRING_"

    .line 12
    .line 13
    invoke-static {p0, p1}, Lf/cz7;->x01(ILjava/lang/String;)Ljava/lang/String;

    .line 14
    .line 15
    .line 16
    move-result-object p0

    .line 17
    return-object p0

    .line 18
    :cond_11
    const/4 p0, 0x0

    .line 19
    const/4 v1, 0x0

    .line 20
    :goto_13
    array-length v2, p1

    .line 21
    if-ge v1, v2, :cond_45

    .line 22
    .line 23
    new-instance v2, Ljava/lang/StringBuilder;

    .line 24
    .line 25
    const-string v3, "\\{"

    .line 26
    .line 27
    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 28
    .line 29
    .line 30
    invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 31
    .line 32
    .line 33
    move-result-object v3

    .line 34
    const/4 v4, 0x1

    .line 35
    new-array v4, v4, [Ljava/lang/Object;

    .line 36
    .line 37
    aput-object v3, v4, p0

    .line 38
    .line 39
    const-string v3, "%1$02X"

    .line 40
    .line 41
    invoke-static {v3, v4}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    .line 42
    .line 43
    .line 44
    move-result-object v3

    .line 45
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 46
    .line 47
    .line 48
    const-string v3, "\\}"

    .line 49
    .line 50
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 51
    .line 52
    .line 53
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 54
    .line 55
    .line 56
    move-result-object v2

    .line 57
    aget-object v3, p1, v1

    .line 58
    .line 59
    invoke-static {v3}, Ljava/util/regex/Matcher;->quoteReplacement(Ljava/lang/String;)Ljava/lang/String;

    .line 60
    .line 61
    .line 62
    move-result-object v3

    .line 63
    invoke-virtual {v0, v2, v3}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 64
    .line 65
    .line 66
    move-result-object v0

    .line 67
    add-int/lit8 v1, v1, 0x1

    .line 68
    .line 69
    goto :goto_13

    .line 70
    :cond_45
    invoke-static {v0}, Lf/gt0;->QW1(Ljava/lang/String;)Ljava/lang/String;

    .line 71
    .line 72
    .line 73
    move-result-object p0

    .line 74
    return-object p0
.end method

.method public static h10(Ljava/lang/String;)Ljava/lang/String;
    .registers 10

    .line 1
    const-string v0, "\\}"

    .line 2
    .line 3
    const-string v1, "STRING_"

    .line 4
    .line 5
    invoke-virtual {p0, v1}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    .line 6
    .line 7
    .line 8
    move-result v2

    .line 9
    if-eqz v2, :cond_5d

    .line 10
    .line 11
    const-string v2, "\\{STRING_"

    .line 12
    .line 13
    invoke-virtual {p0, v2}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    .line 14
    .line 15
    .line 16
    move-result-object v3

    .line 17
    const/4 v4, 0x0

    .line 18
    const/4 v5, 0x0

    .line 19
    :cond_12
    :goto_12
    array-length v6, v3

    .line 20
    add-int/lit8 v6, v6, -0x1

    .line 21
    .line 22
    if-ge v5, v6, :cond_5d

    .line 23
    .line 24
    add-int/lit8 v5, v5, 0x1

    .line 25
    .line 26
    :try_start_19
    aget-object v6, v3, v5

    .line 27
    .line 28
    invoke-virtual {v6, v0}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    .line 29
    .line 30
    .line 31
    move-result-object v6

    .line 32
    aget-object v6, v6, v4

    .line 33
    .line 34
    invoke-static {v6}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    .line 35
    .line 36
    .line 37
    move-result v6

    .line 38
    sget-object v7, Lf/gt0;->bJ1:Lf/k89;

    .line 39
    .line 40
    invoke-virtual {v7, v6}, Lf/x44;->US1(I)Z

    .line 41
    .line 42
    .line 43
    move-result v8

    .line 44
    if-eqz v8, :cond_12

    .line 45
    .line 46
    new-instance v8, Ljava/lang/StringBuilder;

    .line 47
    .line 48
    invoke-direct {v8}, Ljava/lang/StringBuilder;-><init>()V

    .line 49
    .line 50
    .line 51
    invoke-virtual {v8, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 52
    .line 53
    .line 54
    invoke-virtual {v8, v6}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 55
    .line 56
    .line 57
    invoke-virtual {v8, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 58
    .line 59
    .line 60
    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 61
    .line 62
    .line 63
    move-result-object v8

    .line 64
    invoke-virtual {v7, v6}, Lf/k89;->get(I)Ljava/lang/Object;

    .line 65
    .line 66
    .line 67
    move-result-object v7

    .line 68
    check-cast v7, Ljava/lang/String;

    .line 69
    .line 70
    if-nez v7, :cond_53

    .line 71
    .line 72
    new-instance v7, Ljava/lang/StringBuilder;

    .line 73
    .line 74
    invoke-direct {v7, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 75
    .line 76
    .line 77
    invoke-virtual {v7, v6}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 78
    .line 79
    .line 80
    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 81
    .line 82
    .line 83
    move-result-object v7

    .line 84
    :cond_53
    invoke-virtual {p0, v8, v7}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 85
    .line 86
    .line 87
    move-result-object p0
    :try_end_57
    .catch Ljava/lang/Exception; {:try_start_19 .. :try_end_57} :catch_58

    .line 88
    goto :goto_12

    .line 89
    :catch_58
    move-exception v6

    .line 90
    invoke-virtual {v6}, Ljava/lang/Throwable;->printStackTrace()V

    .line 91
    .line 92
    .line 93
    goto :goto_12

    .line 94
    :cond_5d
    return-object p0
.end method

.method public static kN1(Lf/th7;IIIZ)V
    .registers 9

    .line 1
    const/4 v0, 0x1

    .line 2
    if-ge p1, v0, :cond_4

    .line 3
    .line 4
    goto :goto_38

    .line 5
    :cond_4
    invoke-virtual {p0}, Lf/th7;->Kt()Ljava/nio/ByteBuffer;

    .line 6
    .line 7
    .line 8
    move-result-object v1

    .line 9
    invoke-virtual {v1, p1}, Ljava/nio/ByteBuffer;->position(I)Ljava/nio/Buffer;

    .line 10
    .line 11
    .line 12
    const/4 p1, 0x0

    .line 13
    :goto_c
    if-lez p3, :cond_11

    .line 14
    .line 15
    if-gt p3, p1, :cond_11

    .line 16
    .line 17
    goto :goto_38

    .line 18
    :cond_11
    invoke-virtual {v1}, Ljava/nio/ByteBuffer;->getInt()I

    .line 19
    .line 20
    .line 21
    move-result v2

    .line 22
    invoke-static {v2}, Lf/wt2;->In(I)I

    .line 23
    .line 24
    .line 25
    move-result v2

    .line 26
    if-lt v2, v0, :cond_38

    .line 27
    .line 28
    invoke-virtual {v1}, Ljava/nio/Buffer;->limit()I

    .line 29
    .line 30
    .line 31
    move-result v3

    .line 32
    if-le v2, v3, :cond_22

    .line 33
    .line 34
    goto :goto_38

    .line 35
    :cond_22
    invoke-virtual {p0}, Lf/th7;->Kt()Ljava/nio/ByteBuffer;

    .line 36
    .line 37
    .line 38
    move-result-object v3

    .line 39
    invoke-static {v3, v2}, Lf/x25;->xQ(Ljava/nio/ByteBuffer;I)Ljava/lang/String;

    .line 40
    .line 41
    .line 42
    move-result-object v2

    .line 43
    if-eqz p4, :cond_30

    .line 44
    .line 45
    invoke-static {v2}, Lf/ay0;->bJ0(Ljava/lang/String;)Ljava/lang/String;

    .line 46
    .line 47
    .line 48
    move-result-object v2

    .line 49
    :cond_30
    add-int v3, p1, p2

    .line 50
    .line 51
    invoke-static {v3, v2}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 52
    .line 53
    .line 54
    add-int/lit8 p1, p1, 0x1

    .line 55
    .line 56
    goto :goto_c

    .line 57
    :cond_38
    :goto_38
    return-void
.end method

.method public static lPT8(I)Ljava/lang/String;
    .registers 2

    .line 1
    sget-object v0, Lf/gt0;->bJ1:Lf/k89;

    .line 2
    .line 3
    invoke-virtual {v0, p0}, Lf/k89;->get(I)Ljava/lang/Object;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    check-cast v0, Ljava/lang/String;

    .line 8
    .line 9
    if-nez v0, :cond_11

    .line 10
    .line 11
    const-string v0, "STRING_"

    .line 12
    .line 13
    invoke-static {p0, v0}, Lf/cz7;->x01(ILjava/lang/String;)Ljava/lang/String;

    .line 14
    .line 15
    .line 16
    move-result-object p0

    .line 17
    return-object p0

    .line 18
    :cond_11
    invoke-static {v0}, Lf/gt0;->QW1(Ljava/lang/String;)Ljava/lang/String;

    .line 19
    .line 20
    .line 21
    move-result-object p0

    .line 22
    return-object p0
.end method

.method public static oC()V
    .registers 8

    .line 1
    sget-object v0, Lf/an8;->LU:Lf/an8;

    .line 2
    .line 3
    iget-object v0, v0, Lf/an8;->C11:Lf/ry6;

    .line 4
    .line 5
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 6
    .line 7
    .line 8
    iget-object v1, v0, Lf/o76;->QB:[B

    .line 9
    .line 10
    iget-object v2, v0, Lf/tw0;->pb0:[S

    .line 11
    .line 12
    iget-object v0, v0, Lf/ry6;->m6:[S

    .line 13
    .line 14
    array-length v3, v2

    .line 15
    :goto_e
    add-int/lit8 v4, v3, -0x1

    .line 16
    .line 17
    if-lez v3, :cond_31

    .line 18
    .line 19
    aget-byte v3, v1, v4

    .line 20
    .line 21
    const/4 v5, 0x1

    .line 22
    if-ne v3, v5, :cond_2f

    .line 23
    .line 24
    aget-short v3, v2, v4

    .line 25
    .line 26
    aget-short v5, v0, v4

    .line 27
    .line 28
    const v6, 0x3a980

    .line 29
    .line 30
    .line 31
    add-int/2addr v3, v6

    .line 32
    sget-object v7, Lf/gt0;->bJ1:Lf/k89;

    .line 33
    .line 34
    invoke-virtual {v7, v3}, Lf/x44;->US1(I)Z

    .line 35
    .line 36
    .line 37
    move-result v7

    .line 38
    if-nez v7, :cond_2f

    .line 39
    .line 40
    add-int/2addr v5, v6

    .line 41
    invoke-static {v5}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 42
    .line 43
    .line 44
    move-result-object v5

    .line 45
    invoke-static {v3, v5}, Lf/gt0;->Com5(ILjava/lang/String;)V

    .line 46
    .line 47
    .line 48
    :cond_2f
    move v3, v4

    .line 49
    goto :goto_e

    .line 50
    :cond_31
    return-void
.end method

.method public static oN0(IILjava/nio/ByteBuffer;I)V
    .registers 12

    .line 1
    const/4 v0, 0x1

    .line 2
    if-ge p0, v0, :cond_4

    .line 3
    .line 4
    goto :goto_e

    .line 5
    :cond_4
    invoke-virtual {p2, p0}, Ljava/nio/ByteBuffer;->position(I)Ljava/nio/Buffer;

    .line 6
    .line 7
    .line 8
    const/4 p0, 0x0

    .line 9
    const/4 v1, 0x0

    .line 10
    :goto_9
    if-lt v1, p1, :cond_f

    .line 11
    .line 12
    if-gez p1, :cond_e

    .line 13
    .line 14
    goto :goto_f

    .line 15
    :cond_e
    :goto_e
    return-void

    .line 16
    :cond_f
    :goto_f
    const/4 v2, -0x1

    .line 17
    if-gez p1, :cond_29

    .line 18
    .line 19
    invoke-virtual {p2}, Ljava/nio/ByteBuffer;->get()B

    .line 20
    .line 21
    .line 22
    move-result v3

    .line 23
    if-ne v3, v2, :cond_21

    .line 24
    .line 25
    invoke-virtual {p2}, Ljava/nio/Buffer;->position()I

    .line 26
    .line 27
    .line 28
    move-result p0

    .line 29
    sub-int/2addr p0, v0

    .line 30
    invoke-virtual {p2, p0}, Ljava/nio/ByteBuffer;->position(I)Ljava/nio/Buffer;

    .line 31
    .line 32
    .line 33
    return-void

    .line 34
    :cond_21
    invoke-virtual {p2}, Ljava/nio/Buffer;->position()I

    .line 35
    .line 36
    .line 37
    move-result v3

    .line 38
    sub-int/2addr v3, v0

    .line 39
    invoke-virtual {p2, v3}, Ljava/nio/ByteBuffer;->position(I)Ljava/nio/Buffer;

    .line 40
    .line 41
    .line 42
    :cond_29
    add-int/lit8 v3, p3, 0x1

    .line 43
    .line 44
    sget-object v4, Lf/x25;->cH0:Lf/k33;

    .line 45
    .line 46
    new-instance v4, Ljava/lang/StringBuilder;

    .line 47
    .line 48
    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    .line 49
    .line 50
    .line 51
    :cond_32
    :goto_32
    invoke-virtual {p2}, Ljava/nio/ByteBuffer;->get()B

    .line 52
    .line 53
    .line 54
    move-result v5

    .line 55
    if-ne v5, v2, :cond_39

    .line 56
    .line 57
    goto :goto_92

    .line 58
    :cond_39
    const/4 v6, -0x4

    .line 59
    if-ne v5, v6, :cond_5e

    .line 60
    .line 61
    invoke-virtual {p2}, Ljava/nio/ByteBuffer;->get()B

    .line 62
    .line 63
    .line 64
    move-result v5

    .line 65
    const/4 v6, 0x6

    .line 66
    if-ne v5, v6, :cond_47

    .line 67
    .line 68
    :goto_43
    invoke-virtual {p2}, Ljava/nio/ByteBuffer;->get()B

    .line 69
    .line 70
    .line 71
    goto :goto_32

    .line 72
    :cond_47
    const/16 v6, 0x8

    .line 73
    .line 74
    if-ne v5, v6, :cond_4c

    .line 75
    .line 76
    goto :goto_43

    .line 77
    :cond_4c
    const/16 v6, 0xb

    .line 78
    .line 79
    if-ne v5, v6, :cond_54

    .line 80
    .line 81
    :goto_50
    invoke-virtual {p2}, Ljava/nio/ByteBuffer;->getShort()S

    .line 82
    .line 83
    .line 84
    goto :goto_32

    .line 85
    :cond_54
    const/16 v6, 0x10

    .line 86
    .line 87
    if-ne v5, v6, :cond_59

    .line 88
    .line 89
    goto :goto_50

    .line 90
    :cond_59
    const/16 v6, 0x11

    .line 91
    .line 92
    if-ne v5, v6, :cond_32

    .line 93
    .line 94
    goto :goto_43

    .line 95
    :cond_5e
    const/4 v6, -0x3

    .line 96
    if-ne v5, v6, :cond_8a

    .line 97
    .line 98
    invoke-virtual {p2}, Ljava/nio/ByteBuffer;->get()B

    .line 99
    .line 100
    .line 101
    move-result v5

    .line 102
    new-instance v6, Ljava/lang/StringBuilder;

    .line 103
    .line 104
    const-string v7, "{"

    .line 105
    .line 106
    invoke-direct {v6, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 107
    .line 108
    .line 109
    invoke-static {v5}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 110
    .line 111
    .line 112
    move-result-object v5

    .line 113
    new-array v7, v0, [Ljava/lang/Object;

    .line 114
    .line 115
    aput-object v5, v7, p0

    .line 116
    .line 117
    const-string v5, "%1$02X"

    .line 118
    .line 119
    invoke-static {v5, v7}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    .line 120
    .line 121
    .line 122
    move-result-object v5

    .line 123
    invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 124
    .line 125
    .line 126
    const-string v5, "}"

    .line 127
    .line 128
    invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 129
    .line 130
    .line 131
    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 132
    .line 133
    .line 134
    move-result-object v5

    .line 135
    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 136
    .line 137
    .line 138
    goto :goto_32

    .line 139
    :cond_8a
    sget-object v6, Lf/x25;->cH0:Lf/k33;

    .line 140
    .line 141
    invoke-virtual {v6, v5}, Lf/cu2;->FW(B)Z

    .line 142
    .line 143
    .line 144
    move-result v7

    .line 145
    if-nez v7, :cond_9e

    .line 146
    .line 147
    :goto_92
    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 148
    .line 149
    .line 150
    move-result-object v2

    .line 151
    invoke-static {p3, v2}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 152
    .line 153
    .line 154
    add-int/lit8 v1, v1, 0x1

    .line 155
    .line 156
    move p3, v3

    .line 157
    goto/16 :goto_9

    .line 158
    .line 159
    :cond_9e
    invoke-virtual {v6, v5}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 160
    .line 161
    .line 162
    move-result-object v5

    .line 163
    check-cast v5, Ljava/lang/String;

    .line 164
    .line 165
    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 166
    .line 167
    .line 168
    goto :goto_32
.end method

.method public static pS(Lf/th7;IIIZ)V
    .registers 9

    .line 1
    const/4 v0, 0x1

    .line 2
    if-ge p1, v0, :cond_4

    .line 3
    .line 4
    goto :goto_3b

    .line 5
    :cond_4
    invoke-virtual {p0}, Lf/th7;->Kt()Ljava/nio/ByteBuffer;

    .line 6
    .line 7
    .line 8
    move-result-object v1

    .line 9
    invoke-virtual {v1, p1}, Ljava/nio/ByteBuffer;->position(I)Ljava/nio/Buffer;

    .line 10
    .line 11
    .line 12
    const/4 p1, 0x0

    .line 13
    :goto_c
    if-lez p3, :cond_11

    .line 14
    .line 15
    if-gt p3, p1, :cond_11

    .line 16
    .line 17
    goto :goto_3b

    .line 18
    :cond_11
    invoke-virtual {v1}, Ljava/nio/ByteBuffer;->getInt()I

    .line 19
    .line 20
    .line 21
    invoke-virtual {v1}, Ljava/nio/ByteBuffer;->getInt()I

    .line 22
    .line 23
    .line 24
    move-result v2

    .line 25
    invoke-static {v2}, Lf/wt2;->In(I)I

    .line 26
    .line 27
    .line 28
    move-result v2

    .line 29
    if-lt v2, v0, :cond_3b

    .line 30
    .line 31
    invoke-virtual {v1}, Ljava/nio/Buffer;->limit()I

    .line 32
    .line 33
    .line 34
    move-result v3

    .line 35
    if-le v2, v3, :cond_25

    .line 36
    .line 37
    goto :goto_3b

    .line 38
    :cond_25
    invoke-virtual {p0}, Lf/th7;->Kt()Ljava/nio/ByteBuffer;

    .line 39
    .line 40
    .line 41
    move-result-object v3

    .line 42
    invoke-static {v3, v2}, Lf/x25;->xQ(Ljava/nio/ByteBuffer;I)Ljava/lang/String;

    .line 43
    .line 44
    .line 45
    move-result-object v2

    .line 46
    if-eqz p4, :cond_33

    .line 47
    .line 48
    invoke-static {v2}, Lf/ay0;->bJ0(Ljava/lang/String;)Ljava/lang/String;

    .line 49
    .line 50
    .line 51
    move-result-object v2

    .line 52
    :cond_33
    add-int v3, p1, p2

    .line 53
    .line 54
    invoke-static {v3, v2}, Lf/gt0;->V71(ILjava/lang/String;)V

    .line 55
    .line 56
    .line 57
    add-int/lit8 p1, p1, 0x1

    .line 58
    .line 59
    goto :goto_c

    .line 60
    :cond_3b
    :goto_3b
    return-void
.end method

.method public static varargs tt1(BLf/pn2;II[Ljava/lang/String;)Ljava/lang/String;
    .registers 7

    # MonMMO-EX: ROM text the string table supplies. Source 2 (the dex text archive) has no rows for
    # species the ROM never had - category, entry, height and weight rendered "--". A string at
    # 1500000000 + table * 100000 + entry takes the ROM row's place; nothing is written there for a
    # species the ROM owns, so retail text is untouched.
    const/4 v0, 0x2
    if-ne p0, v0, :mmx_rom_text
    if-ltz p3, :mmx_rom_text
    const v0, 0x186a0
    if-ge p3, v0, :mmx_rom_text
    mul-int v1, p2, v0
    add-int/2addr v1, p3
    const v0, 0x59682f00
    add-int/2addr v1, v0
    sget-object v0, Lf/gt0;->bJ1:Lf/k89;
    invoke-virtual {v0, v1}, Lf/k89;->get(I)Ljava/lang/Object;
    move-result-object v0
    if-eqz v0, :mmx_rom_text
    check-cast v0, Ljava/lang/String;
    move-object p0, v0
    const/4 p1, 0x0
    goto :mmx_have_text

    :mmx_rom_text
    .line 1
    sget-object v0, Lf/gt0;->b91:[[Lf/ua7;

    .line 2
    .line 3
    aget-object p0, v0, p0

    .line 4
    .line 5
    iget-byte p1, p1, Lf/pn2;->rm0:B

    .line 6
    .line 7
    aget-object p0, p0, p1

    .line 8
    .line 9
    const/4 p1, 0x0

    .line 10
    invoke-virtual {p0, p2, p1, p3}, Lf/ua7;->dr0(III)Ljava/lang/String;

    .line 11
    .line 12
    .line 13
    move-result-object p0

    :mmx_have_text
    .line 14
    const/4 p2, 0x0

    .line 15
    :goto_e
    array-length p3, p4

    .line 16
    if-ge p2, p3, :cond_40

    .line 17
    .line 18
    new-instance p3, Ljava/lang/StringBuilder;

    .line 19
    .line 20
    const-string v0, "\\{"

    .line 21
    .line 22
    invoke-direct {p3, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 23
    .line 24
    .line 25
    invoke-static {p2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 26
    .line 27
    .line 28
    move-result-object v0

    .line 29
    const/4 v1, 0x1

    .line 30
    new-array v1, v1, [Ljava/lang/Object;

    .line 31
    .line 32
    aput-object v0, v1, p1

    .line 33
    .line 34
    const-string v0, "%1$02X"

    .line 35
    .line 36
    invoke-static {v0, v1}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    .line 37
    .line 38
    .line 39
    move-result-object v0

    .line 40
    invoke-virtual {p3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 41
    .line 42
    .line 43
    const-string v0, "\\}"

    .line 44
    .line 45
    invoke-virtual {p3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 46
    .line 47
    .line 48
    invoke-virtual {p3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 49
    .line 50
    .line 51
    move-result-object p3

    .line 52
    aget-object v0, p4, p2

    .line 53
    .line 54
    invoke-static {v0}, Ljava/util/regex/Matcher;->quoteReplacement(Ljava/lang/String;)Ljava/lang/String;

    .line 55
    .line 56
    .line 57
    move-result-object v0

    .line 58
    invoke-virtual {p0, p3, v0}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 59
    .line 60
    .line 61
    move-result-object p0

    .line 62
    add-int/lit8 p2, p2, 0x1

    .line 63
    .line 64
    goto :goto_e

    .line 65
    :cond_40
    invoke-static {p0}, Lf/gt0;->QW1(Ljava/lang/String;)Ljava/lang/String;

    .line 66
    .line 67
    .line 68
    move-result-object p0

    .line 69
    return-object p0
.end method
