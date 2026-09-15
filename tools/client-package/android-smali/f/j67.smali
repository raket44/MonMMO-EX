.class public final Lf/j67;
.super Lf/w11;

# interfaces
.implements Lf/t64;


# instance fields
.field public final l00:Lf/zp3;

.field public final zc0:Lf/lj6;


# direct methods
.method public constructor <init>(Lf/er7;Lf/zp3;BB)V
    .registers 8

    .line 1
    invoke-static {}, Lf/p37;->N91()V

    .line 2
    .line 3
    .line 4
    const/4 v0, 0x1

    .line 5
    invoke-direct {p0, v0, v0}, Lf/w11;-><init>(ZZ)V

    .line 6
    .line 7
    .line 8
    iput-object p2, p0, Lf/j67;->l00:Lf/zp3;

    .line 9
    .line 10
    const-string v1, "monsterdex-frame"

    .line 11
    .line 12
    invoke-virtual {p0, v1}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 13
    .line 14
    .line 15
    new-instance v1, Lf/yn8;

    .line 16
    .line 17
    const/4 v2, 0x0

    .line 18
    invoke-direct {v1, v2, p2}, Lf/yn8;-><init>(ILf/zp3;)V

    .line 19
    .line 20
    .line 21
    invoke-virtual {p0, v1}, Lf/ul8;->TZ0(Ljava/lang/Runnable;)V

    .line 22
    .line 23
    .line 24
    invoke-virtual {p0, v0}, Lf/ul8;->GZ0(I)V

    .line 25
    .line 26
    .line 27
    new-instance p2, Lf/lj6;

    .line 28
    .line 29
    invoke-direct {p2}, Lf/lj6;-><init>()V

    .line 30
    .line 31
    .line 32
    iput-object p2, p0, Lf/j67;->zc0:Lf/lj6;

    .line 33
    .line 34
    invoke-static {}, Lf/p37;->N91()V

    .line 35
    .line 36
    .line 37
    const/high16 v0, 0x42820000    # 65.0f

    .line 38
    .line 39
    iget-object v1, p2, Lf/lj6;->ir1:Lf/on;

    .line 40
    .line 41
    invoke-virtual {v1, v0}, Lf/on;->TQ1(F)V

    .line 42
    .line 43
    .line 44
    const/high16 v0, 0x425c0000    # 55.0f

    .line 45
    .line 46
    invoke-virtual {v1, v0}, Lf/on;->yW(F)V

    .line 47
    .line 48
    .line 49
    invoke-virtual {p0, p1, p3, p4}, Lf/j67;->cw0(Lf/er7;BB)V

    .line 50
    .line 51
    .line 52
    invoke-virtual {p0, p2}, Lf/rh3;->ec(Lf/rh3;)V

    .line 53
    .line 54
    .line 55
    return-void
.end method


# virtual methods
.method public final cw0(Lf/er7;BB)V
    # MonMMO-EX: one more local (v35) keeps a data evolution's time of day for the badge.
    .registers 40

    .line 1
    move-object/from16 v1, p0

    .line 2
    .line 3
    move-object/from16 v2, p1

    .line 4
    .line 5
    move/from16 v5, p2

    .line 6
    .line 7
    move/from16 v6, p3

    .line 8
    .line 9
    iget-object v9, v1, Lf/j67;->zc0:Lf/lj6;

    .line 10
    .line 11
    invoke-virtual {v9}, Lf/lj6;->tG1()V

    .line 12
    .line 13
    .line 14
    iget-object v0, v1, Lf/j67;->l00:Lf/zp3;

    .line 15
    .line 16
    invoke-virtual {v0, v5}, Lf/zp3;->x1(B)Z

    .line 17
    .line 18
    .line 19
    move-result v3

    .line 20
    if-eqz v3, :cond_21

    .line 21
    .line 22
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 23
    .line 24
    .line 25
    move-result-object v3

    .line 26
    invoke-virtual {v0, v5}, Lf/zp3;->hC1(B)S

    .line 27
    .line 28
    .line 29
    move-result v0

    .line 30
    invoke-virtual {v3, v0}, Lf/y91;->wT0(S)Lf/zp3;

    .line 31
    .line 32
    .line 33
    move-result-object v0

    .line 34
    :cond_21
    move-object v4, v0

    .line 35
    sget-object v0, Lf/x74;->Jg0:Lf/x74;

    .line 36
    .line 37
    iget-short v3, v4, Lf/zp3;->Kj1:S

    .line 38
    .line 39
    invoke-virtual {v4}, Lf/zp3;->nE0()Z

    .line 40
    .line 41
    .line 42
    move-result v7

    .line 43
    invoke-virtual {v2, v0, v3, v5, v7}, Lf/er7;->Om1(Lf/x74;SIZ)Z

    .line 44
    .line 45
    .line 46
    move-result v7

    .line 47
    invoke-virtual {v2, v0, v3}, Lf/er7;->S02(Lf/x74;S)Z

    .line 48
    .line 49
    .line 50
    move-result v0

    .line 51
    sget-object v8, Lf/x74;->rH:Lf/x74;

    .line 52
    .line 53
    invoke-virtual {v4}, Lf/zp3;->nE0()Z

    .line 54
    .line 55
    .line 56
    move-result v10

    .line 57
    invoke-virtual {v2, v8, v3, v5, v10}, Lf/er7;->Om1(Lf/x74;SIZ)Z

    .line 58
    .line 59
    .line 60
    move-result v8

    .line 61
    new-instance v3, Ljava/text/DecimalFormat;

    .line 62
    .line 63
    const-string v10, "000"

    .line 64
    .line 65
    invoke-direct {v3, v10}, Ljava/text/DecimalFormat;-><init>(Ljava/lang/String;)V

    .line 66
    .line 67
    .line 68
    const/16 v10, 0x6a9

    .line 69
    .line 70
    const-string v11, " "

    .line 71
    .line 72
    const/4 v12, 0x0

    .line 73
    if-eqz v0, :cond_75

    .line 74
    .line 75
    new-instance v0, Ljava/lang/StringBuilder;

    .line 76
    .line 77
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    .line 78
    .line 79
    .line 80
    invoke-static {v10}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 81
    .line 82
    .line 83
    move-result-object v10

    .line 84
    invoke-virtual {v0, v10}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 85
    .line 86
    .line 87
    invoke-virtual {v0, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 88
    .line 89
    .line 90
    invoke-virtual {v4, v6}, Lf/zp3;->Wi(B)S

    .line 91
    .line 92
    .line 93
    move-result v10

    .line 94
    int-to-long v13, v10

    .line 95
    invoke-virtual {v3, v13, v14}, Ljava/text/NumberFormat;->format(J)Ljava/lang/String;

    .line 96
    .line 97
    .line 98
    move-result-object v3

    .line 99
    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 100
    .line 101
    .line 102
    invoke-virtual {v0, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 103
    .line 104
    .line 105
    invoke-virtual {v4, v12}, Lf/zp3;->xt(Z)Ljava/lang/String;

    .line 106
    .line 107
    .line 108
    move-result-object v3

    .line 109
    :goto_6c
    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 110
    .line 111
    .line 112
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 113
    .line 114
    .line 115
    move-result-object v0

    .line 116
    move-object v3, v0

    .line 117
    goto :goto_93

    .line 118
    :cond_75
    new-instance v0, Ljava/lang/StringBuilder;

    .line 119
    .line 120
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    .line 121
    .line 122
    .line 123
    invoke-static {v10}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 124
    .line 125
    .line 126
    move-result-object v10

    .line 127
    invoke-virtual {v0, v10}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 128
    .line 129
    .line 130
    invoke-virtual {v0, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 131
    .line 132
    .line 133
    invoke-virtual {v4, v6}, Lf/zp3;->Wi(B)S

    .line 134
    .line 135
    .line 136
    move-result v10

    .line 137
    int-to-long v13, v10

    .line 138
    invoke-virtual {v3, v13, v14}, Ljava/text/NumberFormat;->format(J)Ljava/lang/String;

    .line 139
    .line 140
    .line 141
    move-result-object v3

    .line 142
    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 143
    .line 144
    .line 145
    const-string v3, " ???"

    .line 146
    .line 147
    goto :goto_6c

    .line 148
    :goto_93
    invoke-virtual {v1, v3}, Lf/ul8;->WS(Ljava/lang/String;)V

    .line 149
    .line 150
    .line 151
    new-instance v10, Lf/wt3;

    .line 152
    .line 153
    invoke-direct {v10}, Lf/wt3;-><init>()V

    .line 154
    .line 155
    .line 156
    const/16 v0, 0x6a7

    .line 157
    .line 158
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 159
    .line 160
    .line 161
    move-result-object v13

    .line 162
    new-instance v0, Lf/d73;

    .line 163
    .line 164
    invoke-direct/range {v0 .. v8}, Lf/d73;-><init>(Lf/j67;Lf/er7;Ljava/lang/String;Lf/zp3;BBZZ)V

    .line 165
    .line 166
    .line 167
    invoke-virtual {v10, v0, v13}, Lf/wt3;->Vn0(Lf/rh3;Ljava/lang/String;)Lf/t4;

    .line 168
    .line 169
    .line 170
    const/16 v0, 0x6a8

    .line 171
    .line 172
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 173
    .line 174
    .line 175
    move-result-object v0

    .line 176
    new-instance v3, Lf/x22;

    .line 177
    .line 178
    invoke-direct {v3}, Lf/lj6;-><init>()V

    .line 179
    .line 180
    .line 181
    const-string v7, "dialoglayout"

    .line 182
    .line 183
    invoke-virtual {v3, v7}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 184
    .line 185
    .line 186
    const/4 v13, 0x1

    .line 187
    iput-boolean v13, v3, Lf/lj6;->vn1:Z

    .line 188
    .line 189
    new-instance v14, Lf/cl1;

    .line 190
    .line 191
    invoke-direct {v14}, Lf/iu;-><init>()V

    .line 192
    .line 193
    .line 194
    sget-object v15, Lf/k16;->Sr1:Lf/k16;

    .line 195
    .line 196
    iput-object v15, v14, Lf/cl1;->ER0:Lf/k16;

    .line 197
    .line 198
    iput v13, v14, Lf/cl1;->Oh:I

    .line 199
    .line 200
    new-instance v15, Ljava/util/ArrayList;

    .line 201
    .line 202
    invoke-direct {v15}, Ljava/util/ArrayList;-><init>()V

    .line 203
    .line 204
    .line 205
    iput-object v15, v14, Lf/cl1;->jb:Ljava/util/ArrayList;

    .line 206
    .line 207
    new-instance v15, Lf/mx6;

    .line 208
    .line 209
    invoke-direct {v15}, Ljava/lang/Object;-><init>()V

    .line 210
    .line 211
    .line 212
    new-array v1, v12, [Lf/q27;

    .line 213
    .line 214
    iput-object v1, v15, Lf/mx6;->vf1:[Lf/q27;

    .line 215
    .line 216
    iput-boolean v13, v15, Lf/mx6;->BT1:Z

    .line 217
    .line 218
    iput-object v15, v14, Lf/cl1;->Fo:Lf/mx6;

    .line 219
    .line 220
    invoke-virtual {v14, v15}, Lf/iu;->f30(Lf/n49;)V

    .line 221
    .line 222
    .line 223
    new-instance v1, Lf/b36;

    .line 224
    .line 225
    invoke-direct {v1, v12}, Lf/b36;-><init>(I)V

    .line 226
    .line 227
    .line 228
    const/16 v16, 0x1

    .line 229
    .line 230
    const-class v13, Lf/fq0;

    .line 231
    .line 232
    invoke-virtual {v14, v13, v1}, Lf/zc7;->zN1(Ljava/lang/Class;Lf/my7;)V

    .line 233
    .line 234
    .line 235
    new-instance v1, Lf/Aux;

    .line 236
    .line 237
    invoke-direct {v1, v12}, Lf/Aux;-><init>(I)V

    .line 238
    .line 239
    .line 240
    const-class v13, Lf/uw;

    .line 241
    .line 242
    invoke-virtual {v14, v13, v1}, Lf/zc7;->zN1(Ljava/lang/Class;Lf/my7;)V

    .line 243
    .line 244
    .line 245
    const-string v1, "/dex-move-table"

    .line 246
    .line 247
    invoke-virtual {v14, v1}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 248
    .line 249
    .line 250
    invoke-virtual {v14}, Lf/zc7;->rv1()V

    .line 251
    .line 252
    .line 253
    invoke-virtual {v14}, Lf/zc7;->lj1()V

    .line 254
    .line 255
    .line 256
    iput v12, v14, Lf/rh3;->Lv0:I

    .line 257
    .line 258
    iget-object v1, v14, Lf/cl1;->ER0:Lf/k16;

    .line 259
    .line 260
    iget v1, v1, Lf/k16;->SR:I

    .line 261
    .line 262
    iget v13, v14, Lf/cl1;->Oh:I

    .line 263
    .line 264
    invoke-virtual {v14, v1, v13}, Lf/zc7;->cJ1(II)V

    .line 265
    .line 266
    .line 267
    iput-boolean v8, v15, Lf/mx6;->BT1:Z

    .line 268
    .line 269
    iget-object v1, v4, Lf/zp3;->qy:Ljava/util/ArrayList;

    .line 270
    .line 271
    iget-object v13, v4, Lf/zp3;->KB:[[S

    .line 272
    .line 273
    invoke-virtual {v1}, Ljava/util/ArrayList;->size()I

    .line 274
    .line 275
    .line 276
    move-result v15

    .line 277
    move-object/from16 v18, v13

    .line 278
    .line 279
    :goto_116
    if-ge v12, v15, :cond_13c

    .line 280
    .line 281
    invoke-virtual {v1, v12}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 282
    .line 283
    .line 284
    move-result-object v19

    .line 285
    add-int/lit8 v12, v12, 0x1

    .line 286
    .line 287
    move-object/from16 v13, v19

    .line 288
    .line 289
    check-cast v13, Lf/hj5;

    .line 290
    .line 291
    move-object/from16 v19, v1

    .line 292
    .line 293
    new-instance v1, Lf/q27;

    .line 294
    .line 295
    move/from16 v20, v12

    .line 296
    .line 297
    iget-short v12, v13, Lf/hj5;->Qh1:S

    .line 298
    .line 299
    iget-byte v13, v13, Lf/hj5;->fi0:B

    .line 300
    .line 301
    move/from16 v21, v15

    .line 302
    .line 303
    const/4 v15, 0x0

    .line 304
    invoke-direct {v1, v12, v13, v15, v15}, Lf/q27;-><init>(SBLf/rb8;Lf/ls0;)V

    .line 305
    .line 306
    .line 307
    invoke-virtual {v14, v1}, Lf/cl1;->Rq(Lf/q27;)V

    .line 308
    .line 309
    .line 310
    move-object/from16 v1, v19

    .line 311
    .line 312
    move/from16 v12, v20

    .line 313
    .line 314
    move/from16 v15, v21

    .line 315
    .line 316
    goto :goto_116

    .line 317
    :cond_13c
    new-instance v1, Ljava/util/HashSet;

    .line 318
    .line 319
    invoke-direct {v1}, Ljava/util/HashSet;-><init>()V

    .line 320
    .line 321
    .line 322
    new-instance v12, Lf/m17;

    .line 323
    .line 324
    invoke-direct {v12}, Lf/pl6;-><init>()V

    .line 325
    .line 326
    .line 327
    sget-object v13, Lf/an8;->LU:Lf/an8;

    .line 328
    .line 329
    iget-object v13, v13, Lf/an8;->lO:Ljava/util/TreeMap;

    .line 330
    .line 331
    invoke-virtual {v13}, Ljava/util/TreeMap;->values()Ljava/util/Collection;

    .line 332
    .line 333
    .line 334
    move-result-object v13

    .line 335
    invoke-interface {v13}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    .line 336
    .line 337
    .line 338
    move-result-object v13

    .line 339
    :goto_152
    invoke-interface {v13}, Ljava/util/Iterator;->hasNext()Z

    .line 340
    .line 341
    .line 342
    move-result v15

    .line 343
    if-eqz v15, :cond_1a3

    .line 344
    .line 345
    invoke-interface {v13}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 346
    .line 347
    .line 348
    move-result-object v15

    .line 349
    check-cast v15, Lf/ls0;

    .line 350
    .line 351
    move-object/from16 v19, v13

    .line 352
    .line 353
    iget v13, v15, Lf/ls0;->FU:I

    .line 354
    .line 355
    invoke-static {v13}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 356
    .line 357
    .line 358
    move-result-object v13

    .line 359
    invoke-virtual {v1, v13}, Ljava/util/HashSet;->contains(Ljava/lang/Object;)Z

    .line 360
    .line 361
    .line 362
    move-result v20

    .line 363
    if-eqz v20, :cond_16f

    .line 364
    .line 365
    move-object/from16 v13, v19

    .line 366
    .line 367
    goto :goto_152

    .line 368
    :cond_16f
    invoke-virtual {v1, v13}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    .line 369
    .line 370
    .line 371
    iget-short v13, v15, Lf/ls0;->lj:S

    .line 372
    .line 373
    move-object/from16 v20, v1

    .line 374
    .line 375
    if-lez v13, :cond_196

    .line 376
    .line 377
    sget-object v1, Lf/rb8;->WZ1:Lf/rb8;

    .line 378
    .line 379
    invoke-virtual {v4, v1, v13}, Lf/zp3;->fH0(Lf/rb8;S)Z

    .line 380
    .line 381
    .line 382
    move-result v1

    .line 383
    if-eqz v1, :cond_196

    .line 384
    .line 385
    new-instance v1, Lf/q27;

    .line 386
    .line 387
    iget-short v13, v15, Lf/ls0;->lj:S

    .line 388
    .line 389
    move-object/from16 v21, v9

    .line 390
    .line 391
    move-object/from16 v22, v11

    .line 392
    .line 393
    const/4 v9, 0x0

    .line 394
    const/4 v11, 0x0

    .line 395
    invoke-direct {v1, v13, v11, v9, v15}, Lf/q27;-><init>(SBLf/rb8;Lf/ls0;)V

    .line 396
    .line 397
    .line 398
    invoke-virtual {v14, v1}, Lf/cl1;->Rq(Lf/q27;)V

    .line 399
    .line 400
    .line 401
    iget-short v1, v15, Lf/ls0;->lj:S

    .line 402
    .line 403
    invoke-virtual {v12, v1}, Lf/m17;->Is(S)Z

    .line 404
    .line 405
    .line 406
    goto :goto_19a

    .line 407
    :cond_196
    move-object/from16 v21, v9

    .line 408
    .line 409
    move-object/from16 v22, v11

    .line 410
    .line 411
    :goto_19a
    move-object/from16 v13, v19

    .line 412
    .line 413
    move-object/from16 v1, v20

    .line 414
    .line 415
    move-object/from16 v9, v21

    .line 416
    .line 417
    move-object/from16 v11, v22

    .line 418
    .line 419
    goto :goto_152

    .line 420
    :cond_1a3
    move-object/from16 v21, v9

    .line 421
    .line 422
    move-object/from16 v22, v11

    .line 423
    .line 424
    sget-object v1, Lf/rb8;->ro0:Lf/rb8;

    .line 425
    .line 426
    const/4 v1, 0x4

    .line 427
    aget-object v9, v18, v1

    .line 428
    .line 429
    array-length v11, v9

    .line 430
    const/4 v13, 0x0

    .line 431
    :goto_1ae
    if-ge v13, v11, :cond_1df

    .line 432
    .line 433
    aget-short v15, v9, v13

    .line 434
    .line 435
    invoke-virtual {v12, v15}, Lf/pl6;->ZK1(S)Z

    .line 436
    .line 437
    .line 438
    move-result v19

    .line 439
    if-eqz v19, :cond_1c1

    .line 440
    .line 441
    move-object/from16 v20, v9

    .line 442
    .line 443
    move/from16 v23, v11

    .line 444
    .line 445
    move-object/from16 v24, v12

    .line 446
    .line 447
    const/16 v19, 0x4

    .line 448
    .line 449
    goto :goto_1d5

    .line 450
    :cond_1c1
    const/16 v19, 0x4

    .line 451
    .line 452
    new-instance v1, Lf/q27;

    .line 453
    .line 454
    move-object/from16 v20, v9

    .line 455
    .line 456
    sget-object v9, Lf/rb8;->WZ1:Lf/rb8;

    .line 457
    .line 458
    move/from16 v23, v11

    .line 459
    .line 460
    move-object/from16 v24, v12

    .line 461
    .line 462
    const/4 v11, 0x0

    .line 463
    const/4 v12, 0x0

    .line 464
    invoke-direct {v1, v15, v12, v9, v11}, Lf/q27;-><init>(SBLf/rb8;Lf/ls0;)V

    .line 465
    .line 466
    .line 467
    invoke-virtual {v14, v1}, Lf/cl1;->Rq(Lf/q27;)V

    .line 468
    .line 469
    .line 470
    :goto_1d5
    add-int/lit8 v13, v13, 0x1

    .line 471
    .line 472
    move-object/from16 v9, v20

    .line 473
    .line 474
    move/from16 v11, v23

    .line 475
    .line 476
    move-object/from16 v12, v24

    .line 477
    .line 478
    const/4 v1, 0x4

    .line 479
    goto :goto_1ae

    .line 480
    :cond_1df
    const/16 v19, 0x4

    .line 481
    .line 482
    sget-object v1, Lf/rb8;->ro0:Lf/rb8;

    .line 483
    .line 484
    const/4 v1, 0x3

    .line 485
    aget-object v9, v18, v1

    .line 486
    .line 487
    array-length v11, v9

    .line 488
    const/4 v12, 0x0

    .line 489
    :goto_1e8
    if-ge v12, v11, :cond_206

    .line 490
    .line 491
    aget-short v13, v9, v12

    .line 492
    .line 493
    new-instance v15, Lf/q27;

    .line 494
    .line 495
    const/16 v20, 0x3

    .line 496
    .line 497
    sget-object v1, Lf/rb8;->n60:Lf/rb8;

    .line 498
    .line 499
    move-object/from16 v23, v9

    .line 500
    .line 501
    move/from16 v24, v11

    .line 502
    .line 503
    const/4 v9, 0x0

    .line 504
    const/4 v11, 0x0

    .line 505
    invoke-direct {v15, v13, v11, v1, v9}, Lf/q27;-><init>(SBLf/rb8;Lf/ls0;)V

    .line 506
    .line 507
    .line 508
    invoke-virtual {v14, v15}, Lf/cl1;->Rq(Lf/q27;)V

    .line 509
    .line 510
    .line 511
    add-int/lit8 v12, v12, 0x1

    .line 512
    .line 513
    move-object/from16 v9, v23

    .line 514
    .line 515
    move/from16 v11, v24

    .line 516
    .line 517
    const/4 v1, 0x3

    .line 518
    goto :goto_1e8

    .line 519
    :cond_206
    const/16 v20, 0x3

    .line 520
    .line 521
    new-instance v1, Lf/m17;

    .line 522
    .line 523
    invoke-direct {v1}, Lf/pl6;-><init>()V

    .line 524
    .line 525
    .line 526
    new-instance v9, Lf/m17;

    .line 527
    .line 528
    invoke-direct {v9}, Lf/pl6;-><init>()V

    .line 529
    .line 530
    .line 531
    move-object v11, v4

    .line 532
    :goto_213
    if-eqz v11, :cond_27a

    .line 533
    .line 534
    iget-short v12, v11, Lf/zp3;->Kj1:S

    .line 535
    .line 536
    invoke-virtual {v1, v12}, Lf/pl6;->ZK1(S)Z

    .line 537
    .line 538
    .line 539
    move-result v13

    .line 540
    if-nez v13, :cond_27a

    .line 541
    .line 542
    invoke-virtual {v11, v5}, Lf/zp3;->x1(B)Z

    .line 543
    .line 544
    .line 545
    move-result v13

    .line 546
    if-eqz v13, :cond_230

    .line 547
    .line 548
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 549
    .line 550
    .line 551
    move-result-object v13

    .line 552
    invoke-virtual {v11, v5}, Lf/zp3;->hC1(B)S

    .line 553
    .line 554
    .line 555
    move-result v15

    .line 556
    invoke-virtual {v13, v15}, Lf/y91;->wT0(S)Lf/zp3;

    .line 557
    .line 558
    .line 559
    move-result-object v13

    .line 560
    goto :goto_231

    .line 561
    :cond_230
    move-object v13, v11

    .line 562
    :goto_231
    invoke-virtual {v1, v12}, Lf/m17;->Is(S)Z

    .line 563
    .line 564
    .line 565
    sget-object v12, Lf/rb8;->ro0:Lf/rb8;

    .line 566
    .line 567
    iget-object v12, v13, Lf/zp3;->KB:[[S

    .line 568
    .line 569
    const/4 v13, 0x0

    .line 570
    aget-object v12, v12, v13

    .line 571
    .line 572
    array-length v15, v12

    .line 573
    :goto_23c
    if-ge v13, v15, :cond_273

    .line 574
    .line 575
    move-object/from16 v23, v1

    .line 576
    .line 577
    aget-short v1, v12, v13

    .line 578
    .line 579
    invoke-virtual {v9, v1}, Lf/pl6;->ZK1(S)Z

    .line 580
    .line 581
    .line 582
    move-result v24

    .line 583
    if-eqz v24, :cond_251

    .line 584
    .line 585
    move-object/from16 v24, v9

    .line 586
    .line 587
    move-object/from16 v25, v12

    .line 588
    .line 589
    move/from16 v26, v13

    .line 590
    .line 591
    move/from16 v27, v15

    .line 592
    .line 593
    goto :goto_268

    .line 594
    :cond_251
    invoke-virtual {v9, v1}, Lf/m17;->Is(S)Z

    .line 595
    .line 596
    .line 597
    move-object/from16 v24, v9

    .line 598
    .line 599
    new-instance v9, Lf/q27;

    .line 600
    .line 601
    move-object/from16 v25, v12

    .line 602
    .line 603
    sget-object v12, Lf/rb8;->ro0:Lf/rb8;

    .line 604
    .line 605
    move/from16 v26, v13

    .line 606
    .line 607
    move/from16 v27, v15

    .line 608
    .line 609
    const/4 v13, 0x0

    .line 610
    const/4 v15, 0x0

    .line 611
    invoke-direct {v9, v1, v15, v12, v13}, Lf/q27;-><init>(SBLf/rb8;Lf/ls0;)V

    .line 612
    .line 613
    .line 614
    invoke-virtual {v14, v9}, Lf/cl1;->Rq(Lf/q27;)V

    .line 615
    .line 616
    .line 617
    :goto_268
    add-int/lit8 v13, v26, 0x1

    .line 618
    .line 619
    move-object/from16 v1, v23

    .line 620
    .line 621
    move-object/from16 v9, v24

    .line 622
    .line 623
    move-object/from16 v12, v25

    .line 624
    .line 625
    move/from16 v15, v27

    .line 626
    .line 627
    goto :goto_23c

    .line 628
    :cond_273
    move-object/from16 v23, v1

    .line 629
    .line 630
    move-object/from16 v24, v9

    .line 631
    .line 632
    iget-object v11, v11, Lf/zp3;->U5:Lf/zp3;

    .line 633
    .line 634
    goto :goto_213

    .line 635
    :cond_27a
    sget-object v1, Lf/rb8;->ro0:Lf/rb8;

    .line 636
    .line 637
    aget-object v1, v18, v16

    .line 638
    .line 639
    array-length v9, v1

    .line 640
    const/4 v11, 0x0

    .line 641
    :goto_280
    if-ge v11, v9, :cond_29b

    .line 642
    .line 643
    aget-short v12, v1, v11

    .line 644
    .line 645
    new-instance v13, Lf/q27;

    .line 646
    .line 647
    sget-object v15, Lf/rb8;->VV1:Lf/rb8;

    .line 648
    .line 649
    move-object/from16 v23, v1

    .line 650
    .line 651
    move/from16 v24, v9

    .line 652
    .line 653
    const/4 v1, 0x0

    .line 654
    const/4 v9, 0x0

    .line 655
    invoke-direct {v13, v12, v9, v15, v1}, Lf/q27;-><init>(SBLf/rb8;Lf/ls0;)V

    .line 656
    .line 657
    .line 658
    invoke-virtual {v14, v13}, Lf/cl1;->Rq(Lf/q27;)V

    .line 659
    .line 660
    .line 661
    add-int/lit8 v11, v11, 0x1

    .line 662
    .line 663
    move-object/from16 v1, v23

    .line 664
    .line 665
    move/from16 v9, v24

    .line 666
    .line 667
    goto :goto_280

    .line 668
    :cond_29b
    sget-object v1, Lf/rb8;->ro0:Lf/rb8;

    .line 669
    .line 670
    const/4 v1, 0x2

    .line 671
    aget-object v9, v18, v1

    .line 672
    .line 673
    array-length v11, v9

    .line 674
    const/4 v12, 0x0

    .line 675
    :goto_2a2
    if-ge v12, v11, :cond_2be

    .line 676
    .line 677
    aget-short v13, v9, v12

    .line 678
    .line 679
    new-instance v15, Lf/q27;

    .line 680
    .line 681
    sget-object v1, Lf/rb8;->pP0:Lf/rb8;

    .line 682
    .line 683
    move-object/from16 v24, v9

    .line 684
    .line 685
    move/from16 v25, v11

    .line 686
    .line 687
    const/4 v9, 0x0

    .line 688
    const/4 v11, 0x0

    .line 689
    invoke-direct {v15, v13, v11, v1, v9}, Lf/q27;-><init>(SBLf/rb8;Lf/ls0;)V

    .line 690
    .line 691
    .line 692
    invoke-virtual {v14, v15}, Lf/cl1;->Rq(Lf/q27;)V

    .line 693
    .line 694
    .line 695
    add-int/lit8 v12, v12, 0x1

    .line 696
    .line 697
    move-object/from16 v9, v24

    .line 698
    .line 699
    move/from16 v11, v25

    .line 700
    .line 701
    const/4 v1, 0x2

    .line 702
    goto :goto_2a2

    .line 703
    :cond_2be
    sget-object v1, Lf/rb8;->ro0:Lf/rb8;

    .line 704
    .line 705
    const/4 v1, 0x5

    .line 706
    aget-object v9, v18, v1

    .line 707
    .line 708
    array-length v11, v9

    .line 709
    const/4 v12, 0x0

    .line 710
    :goto_2c5
    if-ge v12, v11, :cond_2e3

    .line 711
    .line 712
    aget-short v13, v9, v12

    .line 713
    .line 714
    new-instance v15, Lf/q27;

    .line 715
    .line 716
    const/16 v24, 0x5

    .line 717
    .line 718
    sget-object v1, Lf/rb8;->WD1:Lf/rb8;

    .line 719
    .line 720
    move-object/from16 v25, v9

    .line 721
    .line 722
    move/from16 v26, v11

    .line 723
    .line 724
    const/4 v9, 0x0

    .line 725
    const/4 v11, 0x0

    .line 726
    invoke-direct {v15, v13, v11, v1, v9}, Lf/q27;-><init>(SBLf/rb8;Lf/ls0;)V

    .line 727
    .line 728
    .line 729
    invoke-virtual {v14, v15}, Lf/cl1;->Rq(Lf/q27;)V

    .line 730
    .line 731
    .line 732
    add-int/lit8 v12, v12, 0x1

    .line 733
    .line 734
    move-object/from16 v9, v25

    .line 735
    .line 736
    move/from16 v11, v26

    .line 737
    .line 738
    const/4 v1, 0x5

    .line 739
    goto :goto_2c5

    .line 740
    :cond_2e3
    const/16 v24, 0x5

    .line 741
    .line 742
    sget-object v1, Lf/rb8;->ro0:Lf/rb8;

    .line 743
    .line 744
    const/4 v1, 0x6

    .line 745
    aget-object v9, v18, v1

    .line 746
    .line 747
    array-length v11, v9

    .line 748
    const/4 v12, 0x0

    .line 749
    :goto_2ec
    if-ge v12, v11, :cond_30a

    .line 750
    .line 751
    aget-short v13, v9, v12

    .line 752
    .line 753
    new-instance v15, Lf/q27;

    .line 754
    .line 755
    const/16 v18, 0x6

    .line 756
    .line 757
    sget-object v1, Lf/rb8;->sR1:Lf/rb8;

    .line 758
    .line 759
    move-object/from16 v25, v9

    .line 760
    .line 761
    move/from16 v26, v11

    .line 762
    .line 763
    const/4 v9, 0x0

    .line 764
    const/4 v11, 0x0

    .line 765
    invoke-direct {v15, v13, v11, v1, v9}, Lf/q27;-><init>(SBLf/rb8;Lf/ls0;)V

    .line 766
    .line 767
    .line 768
    invoke-virtual {v14, v15}, Lf/cl1;->Rq(Lf/q27;)V

    .line 769
    .line 770
    .line 771
    add-int/lit8 v12, v12, 0x1

    .line 772
    .line 773
    move-object/from16 v9, v25

    .line 774
    .line 775
    move/from16 v11, v26

    .line 776
    .line 777
    const/4 v1, 0x6

    .line 778
    goto :goto_2ec

    .line 779
    :cond_30a
    const/16 v18, 0x6

    .line 780
    .line 781
    invoke-virtual {v14}, Lf/cl1;->G7()V

    .line 782
    .line 783
    .line 784
    new-instance v1, Lf/mw0;

    .line 785
    .line 786
    invoke-direct {v1, v14}, Lf/mw0;-><init>(Lf/rh3;)V

    .line 787
    .line 788
    .line 789
    const/4 v9, 0x2

    .line 790
    invoke-virtual {v1, v9}, Lf/mw0;->si1(I)V

    .line 791
    .line 792
    .line 793
    iget-object v9, v3, Lf/lj6;->ir1:Lf/on;

    .line 794
    .line 795
    invoke-virtual {v9}, Lf/on;->TN1()Lf/un0;

    .line 796
    .line 797
    .line 798
    move-result-object v11

    .line 799
    invoke-virtual {v11}, Lf/un0;->Go1()V

    .line 800
    .line 801
    .line 802
    invoke-virtual {v9, v1}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 803
    .line 804
    .line 805
    move-result-object v1

    .line 806
    invoke-virtual {v1}, Lf/un0;->sY1()V

    .line 807
    .line 808
    .line 809
    invoke-virtual {v9}, Lf/on;->TN1()Lf/un0;

    .line 810
    .line 811
    .line 812
    move-result-object v1

    .line 813
    invoke-virtual {v1}, Lf/un0;->Go1()V

    .line 814
    .line 815
    .line 816
    invoke-virtual {v10, v3, v0}, Lf/wt3;->Vn0(Lf/rh3;Ljava/lang/String;)Lf/t4;

    .line 817
    .line 818
    .line 819
    const/16 v0, 0x6b5

    .line 820
    .line 821
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 822
    .line 823
    .line 824
    move-result-object v0

    .line 825
    new-instance v1, Lf/mh5;

    .line 826
    .line 827
    sget-object v3, Lf/mh5;->f9:[I

    .line 828
    .line 829
    invoke-direct {v1}, Lf/lj6;-><init>()V

    .line 830
    .line 831
    .line 832
    new-instance v9, Lf/lj6;

    .line 833
    .line 834
    invoke-direct {v9}, Lf/lj6;-><init>()V

    .line 835
    .line 836
    .line 837
    const-string v11, "label-area-monsterdex"

    .line 838
    .line 839
    invoke-virtual {v9, v11}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 840
    .line 841
    .line 842
    const/4 v12, 0x1

    .line 843
    iput-boolean v12, v9, Lf/lj6;->vn1:Z

    .line 844
    .line 845
    sget-object v13, Lf/r59;->hd0:[Lf/r59;

    .line 846
    .line 847
    array-length v13, v13

    .line 848
    add-int/2addr v13, v12

    .line 849
    new-array v12, v13, [Lf/xd2;

    .line 850
    .line 851
    const/4 v14, 0x0

    .line 852
    :goto_353
    if-ge v14, v13, :cond_35f

    .line 853
    .line 854
    new-instance v15, Lf/xd2;

    .line 855
    .line 856
    invoke-direct {v15}, Lf/xd2;-><init>()V

    .line 857
    .line 858
    .line 859
    aput-object v15, v12, v14

    .line 860
    .line 861
    add-int/lit8 v14, v14, 0x1

    .line 862
    .line 863
    goto :goto_353

    .line 864
    :cond_35f
    const/16 v17, 0x0

    .line 865
    .line 866
    aget-object v14, v12, v17

    .line 867
    .line 868
    const-string v15, "progressbar-hp"

    .line 869
    .line 870
    invoke-virtual {v14, v15}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 871
    .line 872
    .line 873
    const/16 v16, 0x1

    .line 874
    .line 875
    aget-object v14, v12, v16

    .line 876
    .line 877
    const-string v15, "progressbar-attack"

    .line 878
    .line 879
    invoke-virtual {v14, v15}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 880
    .line 881
    .line 882
    const/16 v23, 0x2

    .line 883
    .line 884
    aget-object v14, v12, v23

    .line 885
    .line 886
    const-string v15, "progressbar-defense"

    .line 887
    .line 888
    invoke-virtual {v14, v15}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 889
    .line 890
    .line 891
    aget-object v14, v12, v20

    .line 892
    .line 893
    const-string v15, "progressbar-spatk"

    .line 894
    .line 895
    invoke-virtual {v14, v15}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 896
    .line 897
    .line 898
    aget-object v14, v12, v19

    .line 899
    .line 900
    const-string v15, "progressbar-spdef"

    .line 901
    .line 902
    invoke-virtual {v14, v15}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 903
    .line 904
    .line 905
    aget-object v14, v12, v24

    .line 906
    .line 907
    const-string v15, "progressbar-speed"

    .line 908
    .line 909
    invoke-virtual {v14, v15}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 910
    .line 911
    .line 912
    aget-object v14, v12, v18

    .line 913
    .line 914
    const-string v15, "progressbar-total"

    .line 915
    .line 916
    invoke-virtual {v14, v15}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 917
    .line 918
    .line 919
    const/4 v14, 0x0

    .line 920
    :goto_397
    const-string v15, ""

    .line 921
    .line 922
    move-object/from16 v25, v3

    .line 923
    .line 924
    iget-object v3, v9, Lf/lj6;->ir1:Lf/on;

    .line 925
    .line 926
    if-ge v14, v13, :cond_498

    .line 927
    .line 928
    move-object/from16 v26, v12

    .line 929
    .line 930
    add-int/lit8 v12, v14, 0x1

    .line 931
    .line 932
    move/from16 v27, v14

    .line 933
    .line 934
    new-instance v14, Lf/h95;

    .line 935
    .line 936
    if-ge v12, v13, :cond_3b9

    .line 937
    .line 938
    sget-object v28, Lf/r59;->hd0:[Lf/r59;

    .line 939
    .line 940
    aget-object v2, v28, v27

    .line 941
    .line 942
    iget-byte v2, v2, Lf/r59;->e:B

    .line 943
    .line 944
    add-int/lit16 v2, v2, 0x6b8

    .line 945
    .line 946
    invoke-static {v2}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 947
    .line 948
    .line 949
    move-result-object v2

    .line 950
    invoke-direct {v14, v2}, Lf/h95;-><init>(Ljava/lang/String;)V

    .line 951
    .line 952
    .line 953
    goto :goto_3c5

    .line 954
    :cond_3b9
    sget-object v2, Lf/r59;->hd0:[Lf/r59;

    .line 955
    .line 956
    array-length v2, v2

    .line 957
    add-int/lit16 v2, v2, 0x6b8

    .line 958
    .line 959
    invoke-static {v2}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 960
    .line 961
    .line 962
    move-result-object v2

    .line 963
    invoke-direct {v14, v2}, Lf/h95;-><init>(Ljava/lang/String;)V

    .line 964
    .line 965
    .line 966
    :goto_3c5
    new-instance v2, Lf/h95;

    .line 967
    .line 968
    const/4 v6, 0x0

    .line 969
    invoke-direct {v2, v6}, Lf/h95;-><init>(Lf/er7;)V

    .line 970
    .line 971
    .line 972
    if-eqz v8, :cond_432

    .line 973
    .line 974
    if-ge v12, v13, :cond_3f4

    .line 975
    .line 976
    sget-object v6, Lf/r59;->hd0:[Lf/r59;

    .line 977
    .line 978
    aget-object v15, v6, v27

    .line 979
    .line 980
    invoke-virtual {v4, v15}, Lf/zp3;->xA1(Lf/r59;)I

    .line 981
    .line 982
    .line 983
    move-result v15

    .line 984
    invoke-static {v15}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;

    .line 985
    .line 986
    .line 987
    move-result-object v15

    .line 988
    invoke-virtual {v2, v15}, Lf/h95;->BR0(Ljava/lang/String;)V

    .line 989
    .line 990
    .line 991
    aget-object v15, v26, v27

    .line 992
    .line 993
    aget-object v6, v6, v27

    .line 994
    .line 995
    invoke-virtual {v4, v6}, Lf/zp3;->xA1(Lf/r59;)I

    .line 996
    .line 997
    .line 998
    move-result v6

    .line 999
    int-to-float v6, v6

    .line 1000
    move/from16 v28, v6

    .line 1001
    .line 1002
    aget v6, v25, v27

    .line 1003
    .line 1004
    int-to-float v6, v6

    .line 1005
    div-float v6, v28, v6

    .line 1006
    .line 1007
    invoke-virtual {v15, v6}, Lf/xd2;->e4(F)V

    .line 1008
    .line 1009
    .line 1010
    move/from16 v28, v12

    .line 1011
    .line 1012
    goto :goto_43d

    .line 1013
    :cond_3f4
    new-instance v6, Ljava/lang/StringBuilder;

    .line 1014
    .line 1015
    invoke-direct {v6, v15}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 1016
    .line 1017
    .line 1018
    iget v15, v4, Lf/zp3;->sw:I

    .line 1019
    .line 1020
    move/from16 v28, v12

    .line 1021
    .line 1022
    iget v12, v4, Lf/zp3;->NU0:I

    .line 1023
    .line 1024
    add-int/2addr v15, v12

    .line 1025
    iget v12, v4, Lf/zp3;->IT1:I

    .line 1026
    .line 1027
    add-int/2addr v15, v12

    .line 1028
    iget v12, v4, Lf/zp3;->sR1:I

    .line 1029
    .line 1030
    add-int/2addr v15, v12

    .line 1031
    iget v12, v4, Lf/zp3;->lZ:I

    .line 1032
    .line 1033
    add-int/2addr v15, v12

    .line 1034
    iget v12, v4, Lf/zp3;->hP0:I

    .line 1035
    .line 1036
    add-int/2addr v15, v12

    .line 1037
    invoke-virtual {v6, v15}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 1038
    .line 1039
    .line 1040
    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 1041
    .line 1042
    .line 1043
    move-result-object v6

    .line 1044
    invoke-virtual {v2, v6}, Lf/h95;->BR0(Ljava/lang/String;)V

    .line 1045
    .line 1046
    .line 1047
    aget-object v6, v26, v27

    .line 1048
    .line 1049
    iget v12, v4, Lf/zp3;->sw:I

    .line 1050
    .line 1051
    iget v15, v4, Lf/zp3;->NU0:I

    .line 1052
    .line 1053
    add-int/2addr v12, v15

    .line 1054
    iget v15, v4, Lf/zp3;->IT1:I

    .line 1055
    .line 1056
    add-int/2addr v12, v15

    .line 1057
    iget v15, v4, Lf/zp3;->sR1:I

    .line 1058
    .line 1059
    add-int/2addr v12, v15

    .line 1060
    iget v15, v4, Lf/zp3;->lZ:I

    .line 1061
    .line 1062
    add-int/2addr v12, v15

    .line 1063
    iget v15, v4, Lf/zp3;->hP0:I

    .line 1064
    .line 1065
    add-int/2addr v12, v15

    .line 1066
    int-to-float v12, v12

    .line 1067
    aget v15, v25, v18

    .line 1068
    .line 1069
    int-to-float v15, v15

    .line 1070
    div-float/2addr v12, v15

    .line 1071
    :goto_42e
    invoke-virtual {v6, v12}, Lf/xd2;->e4(F)V

    .line 1072
    .line 1073
    .line 1074
    goto :goto_43d

    .line 1075
    :cond_432
    move/from16 v28, v12

    .line 1076
    .line 1077
    const-string v6, "?"

    .line 1078
    .line 1079
    invoke-virtual {v2, v6}, Lf/h95;->BR0(Ljava/lang/String;)V

    .line 1080
    .line 1081
    .line 1082
    aget-object v6, v26, v27

    .line 1083
    .line 1084
    const/4 v12, 0x0

    .line 1085
    goto :goto_42e

    .line 1086
    :goto_43d
    new-instance v6, Lf/lj6;

    .line 1087
    .line 1088
    invoke-direct {v6}, Lf/lj6;-><init>()V

    .line 1089
    .line 1090
    .line 1091
    const/4 v12, 0x1

    .line 1092
    iput-boolean v12, v6, Lf/lj6;->vn1:Z

    .line 1093
    .line 1094
    const-string v12, "stat-widget"

    .line 1095
    .line 1096
    invoke-virtual {v6, v12}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 1097
    .line 1098
    .line 1099
    iget-object v12, v6, Lf/lj6;->ir1:Lf/on;

    .line 1100
    .line 1101
    aget-object v15, v26, v27

    .line 1102
    .line 1103
    invoke-virtual {v12, v15}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 1104
    .line 1105
    .line 1106
    move-result-object v12

    .line 1107
    invoke-virtual {v12}, Lf/un0;->DL0()V

    .line 1108
    .line 1109
    .line 1110
    new-instance v12, Lf/lj6;

    .line 1111
    .line 1112
    invoke-direct {v12}, Lf/lj6;-><init>()V

    .line 1113
    .line 1114
    .line 1115
    iget-object v15, v12, Lf/lj6;->ir1:Lf/on;

    .line 1116
    .line 1117
    move/from16 v27, v13

    .line 1118
    .line 1119
    iget-object v13, v15, Lf/on;->I91:Lf/un0;

    .line 1120
    .line 1121
    invoke-static {}, Lf/p37;->N91()V

    .line 1122
    .line 1123
    .line 1124
    move/from16 v29, v8

    .line 1125
    .line 1126
    const/high16 v8, 0x40a00000    # 5.0f

    .line 1127
    .line 1128
    invoke-virtual {v13, v8}, Lf/un0;->zv0(F)V

    .line 1129
    .line 1130
    .line 1131
    invoke-virtual {v15, v14}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 1132
    .line 1133
    .line 1134
    move-result-object v13

    .line 1135
    invoke-virtual {v13}, Lf/un0;->Te1()V

    .line 1136
    .line 1137
    .line 1138
    invoke-virtual {v15, v2}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 1139
    .line 1140
    .line 1141
    move-result-object v2

    .line 1142
    invoke-virtual {v2}, Lf/un0;->Te1()V

    .line 1143
    .line 1144
    .line 1145
    invoke-virtual {v15, v6}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 1146
    .line 1147
    .line 1148
    move-result-object v2

    .line 1149
    invoke-virtual {v2}, Lf/un0;->Te1()V

    .line 1150
    .line 1151
    .line 1152
    invoke-virtual {v3, v12}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 1153
    .line 1154
    .line 1155
    move-result-object v2

    .line 1156
    invoke-static {}, Lf/p37;->N91()V

    .line 1157
    .line 1158
    .line 1159
    invoke-virtual {v2, v8}, Lf/un0;->zv0(F)V

    .line 1160
    .line 1161
    .line 1162
    invoke-virtual {v9}, Lf/lj6;->ub()Lf/un0;

    .line 1163
    .line 1164
    .line 1165
    move-object/from16 v3, v25

    .line 1166
    .line 1167
    move-object/from16 v12, v26

    .line 1168
    .line 1169
    move/from16 v13, v27

    .line 1170
    .line 1171
    move/from16 v14, v28

    .line 1172
    .line 1173
    move/from16 v8, v29

    .line 1174
    .line 1175
    goto/16 :goto_397

    .line 1176
    .line 1177
    :cond_498
    move/from16 v29, v8

    .line 1178
    .line 1179
    new-instance v2, Lf/rh3;

    .line 1180
    .line 1181
    invoke-direct {v2}, Lf/rh3;-><init>()V

    .line 1182
    .line 1183
    .line 1184
    invoke-virtual {v3, v2}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 1185
    .line 1186
    .line 1187
    move-result-object v2

    .line 1188
    invoke-virtual {v2}, Lf/un0;->Qg0()V

    .line 1189
    .line 1190
    .line 1191
    iget-object v2, v1, Lf/lj6;->ir1:Lf/on;

    .line 1192
    .line 1193
    invoke-virtual {v2, v9}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 1194
    .line 1195
    .line 1196
    move-result-object v2

    .line 1197
    invoke-virtual {v2}, Lf/un0;->sY1()V

    .line 1198
    .line 1199
    .line 1200
    invoke-virtual {v10, v1, v0}, Lf/wt3;->Vn0(Lf/rh3;Ljava/lang/String;)Lf/t4;

    .line 1201
    .line 1202
    .line 1203
    const/16 v0, 0x1944

    .line 1204
    .line 1205
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 1206
    .line 1207
    .line 1208
    move-result-object v0

    .line 1209
    new-instance v1, Lf/la;

    .line 1210
    .line 1211
    invoke-direct {v1}, Lf/lj6;-><init>()V

    .line 1212
    .line 1213
    .line 1214
    invoke-virtual {v1, v7}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 1215
    .line 1216
    .line 1217
    const/4 v12, 0x1

    .line 1218
    iput-boolean v12, v1, Lf/lj6;->vn1:Z

    .line 1219
    .line 1220
    invoke-virtual {v4, v5}, Lf/zp3;->ef(B)Lf/eb6;

    .line 1221
    .line 1222
    .line 1223
    move-result-object v2

    .line 1224
    invoke-virtual {v4, v5}, Lf/zp3;->ch(B)Lf/eb6;

    .line 1225
    .line 1226
    .line 1227
    move-result-object v3

    .line 1228
    if-eq v2, v3, :cond_4cf

    .line 1229
    .line 1230
    const/4 v6, 0x1

    .line 1231
    goto :goto_4d0

    .line 1232
    :cond_4cf
    const/4 v6, 0x0

    .line 1233
    :goto_4d0
    new-instance v7, Ljava/util/ArrayList;

    .line 1234
    .line 1235
    invoke-direct {v7}, Ljava/util/ArrayList;-><init>()V

    .line 1236
    .line 1237
    .line 1238
    new-instance v8, Ljava/util/ArrayList;

    .line 1239
    .line 1240
    invoke-direct {v8}, Ljava/util/ArrayList;-><init>()V

    .line 1241
    .line 1242
    .line 1243
    new-instance v9, Ljava/util/ArrayList;

    .line 1244
    .line 1245
    invoke-direct {v9}, Ljava/util/ArrayList;-><init>()V

    .line 1246
    .line 1247
    .line 1248
    new-instance v12, Ljava/util/ArrayList;

    .line 1249
    .line 1250
    invoke-direct {v12}, Ljava/util/ArrayList;-><init>()V

    .line 1251
    .line 1252
    .line 1253
    new-instance v13, Ljava/util/ArrayList;

    .line 1254
    .line 1255
    invoke-direct {v13}, Ljava/util/ArrayList;-><init>()V

    .line 1256
    .line 1257
    .line 1258
    new-instance v14, Ljava/util/ArrayList;

    .line 1259
    .line 1260
    invoke-direct {v14}, Ljava/util/ArrayList;-><init>()V

    .line 1261
    .line 1262
    .line 1263
    if-eqz v29, :cond_582

    .line 1264
    .line 1265
    move/from16 v25, v6

    .line 1266
    .line 1267
    sget-object v6, Lf/eb6;->gR0:[Lf/eb6;

    .line 1268
    .line 1269
    move-object/from16 v26, v4

    .line 1270
    .line 1271
    array-length v4, v6

    .line 1272
    move-object/from16 v27, v6

    .line 1273
    .line 1274
    const/4 v6, 0x0

    .line 1275
    :goto_4fa
    if-ge v6, v4, :cond_586

    .line 1276
    .line 1277
    move/from16 v28, v4

    .line 1278
    .line 1279
    aget-object v4, v27, v6

    .line 1280
    .line 1281
    move/from16 v30, v6

    .line 1282
    .line 1283
    sget-object v6, Lf/eb6;->Nu:Lf/eb6;

    .line 1284
    .line 1285
    if-eq v4, v6, :cond_50a

    .line 1286
    .line 1287
    sget-object v6, Lf/eb6;->nw0:Lf/eb6;

    .line 1288
    .line 1289
    if-ne v4, v6, :cond_50f

    .line 1290
    .line 1291
    :cond_50a
    move-object v6, v2

    .line 1292
    move-object/from16 v31, v3

    .line 1293
    .line 1294
    goto/16 :goto_577

    .line 1295
    .line 1296
    :cond_50f
    const/4 v6, 0x0

    .line 1297
    if-eqz v25, :cond_522

    .line 1298
    .line 1299
    invoke-virtual {v4, v2, v6}, Lf/eb6;->Cj(Lf/eb6;Z)D

    .line 1300
    .line 1301
    .line 1302
    move-result-wide v31

    .line 1303
    invoke-virtual {v4, v3, v6}, Lf/eb6;->Cj(Lf/eb6;Z)D

    .line 1304
    .line 1305
    .line 1306
    move-result-wide v33

    .line 1307
    mul-double v33, v33, v31

    .line 1308
    .line 1309
    :goto_51c
    move-object v6, v2

    .line 1310
    move-object/from16 v31, v3

    .line 1311
    .line 1312
    move-wide/from16 v2, v33

    .line 1313
    .line 1314
    goto :goto_527

    .line 1315
    :cond_522
    invoke-virtual {v4, v2, v6}, Lf/eb6;->Cj(Lf/eb6;Z)D

    .line 1316
    .line 1317
    .line 1318
    move-result-wide v33

    .line 1319
    goto :goto_51c

    .line 1320
    :goto_527
    const-wide/high16 v32, 0x4010000000000000L    # 4.0

    .line 1321
    .line 1322
    cmpl-double v34, v2, v32

    .line 1323
    .line 1324
    if-nez v34, :cond_531

    .line 1325
    .line 1326
    invoke-virtual {v7, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 1327
    .line 1328
    .line 1329
    goto :goto_577

    .line 1330
    :cond_531
    const-wide/high16 v32, 0x4000000000000000L    # 2.0

    .line 1331
    .line 1332
    cmpl-double v34, v2, v32

    .line 1333
    .line 1334
    if-nez v34, :cond_53b

    .line 1335
    .line 1336
    invoke-virtual {v8, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 1337
    .line 1338
    .line 1339
    goto :goto_577

    .line 1340
    :cond_53b
    const-wide/high16 v32, 0x3ff0000000000000L    # 1.0

    .line 1341
    .line 1342
    cmpl-double v34, v2, v32

    .line 1343
    .line 1344
    if-nez v34, :cond_545

    .line 1345
    .line 1346
    invoke-virtual {v9, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 1347
    .line 1348
    .line 1349
    goto :goto_577

    .line 1350
    :cond_545
    const-wide/high16 v32, 0x3fe0000000000000L    # 0.5

    .line 1351
    .line 1352
    cmpl-double v34, v2, v32

    .line 1353
    .line 1354
    if-nez v34, :cond_54f

    .line 1355
    .line 1356
    invoke-virtual {v12, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 1357
    .line 1358
    .line 1359
    goto :goto_577

    .line 1360
    :cond_54f
    const-wide/high16 v32, 0x3fd0000000000000L    # 0.25

    .line 1361
    .line 1362
    cmpl-double v34, v2, v32

    .line 1363
    .line 1364
    if-nez v34, :cond_559

    .line 1365
    .line 1366
    invoke-virtual {v13, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 1367
    .line 1368
    .line 1369
    goto :goto_577

    .line 1370
    :cond_559
    const-wide/16 v32, 0x0

    .line 1371
    .line 1372
    cmpl-double v34, v2, v32

    .line 1373
    .line 1374
    if-nez v34, :cond_563

    .line 1375
    .line 1376
    invoke-virtual {v14, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 1377
    .line 1378
    .line 1379
    goto :goto_577

    .line 1380
    :cond_563
    new-instance v0, Ljava/lang/IllegalArgumentException;

    .line 1381
    .line 1382
    new-instance v1, Ljava/lang/StringBuilder;

    .line 1383
    .line 1384
    const-string v4, "Why??? "

    .line 1385
    .line 1386
    invoke-direct {v1, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 1387
    .line 1388
    .line 1389
    invoke-virtual {v1, v2, v3}, Ljava/lang/StringBuilder;->append(D)Ljava/lang/StringBuilder;

    .line 1390
    .line 1391
    .line 1392
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 1393
    .line 1394
    .line 1395
    move-result-object v1

    .line 1396
    invoke-direct {v0, v1}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    .line 1397
    .line 1398
    .line 1399
    throw v0

    .line 1400
    :goto_577
    add-int/lit8 v2, v30, 0x1

    .line 1401
    .line 1402
    move-object v3, v6

    .line 1403
    move v6, v2

    .line 1404
    move-object v2, v3

    .line 1405
    move/from16 v4, v28

    .line 1406
    .line 1407
    move-object/from16 v3, v31

    .line 1408
    .line 1409
    goto/16 :goto_4fa

    .line 1410
    .line 1411
    :cond_582
    move-object/from16 v26, v4

    .line 1412
    .line 1413
    move/from16 v25, v6

    .line 1414
    .line 1415
    :cond_586
    new-instance v2, Lf/lj6;

    .line 1416
    .line 1417
    invoke-direct {v2}, Lf/lj6;-><init>()V

    .line 1418
    .line 1419
    .line 1420
    const/4 v3, 0x1

    .line 1421
    iput-boolean v3, v2, Lf/lj6;->vn1:Z

    .line 1422
    .line 1423
    invoke-virtual {v2, v11}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 1424
    .line 1425
    .line 1426
    iget-object v3, v2, Lf/lj6;->ir1:Lf/on;

    .line 1427
    .line 1428
    iget-object v4, v3, Lf/on;->I91:Lf/un0;

    .line 1429
    .line 1430
    const/high16 v6, 0x40400000    # 3.0f

    .line 1431
    .line 1432
    invoke-virtual {v4, v6}, Lf/un0;->zv0(F)V

    .line 1433
    .line 1434
    .line 1435
    const-string v4, "label-title-dex-long-but-not-to-long"

    .line 1436
    .line 1437
    invoke-virtual {v3, v15, v4}, Lf/on;->rc(Ljava/lang/String;Ljava/lang/String;)Lf/un0;

    .line 1438
    .line 1439
    .line 1440
    move-result-object v4

    .line 1441
    invoke-virtual {v4}, Lf/un0;->Jr1()V

    .line 1442
    .line 1443
    .line 1444
    const/16 v4, 0x193d

    .line 1445
    .line 1446
    const-string v6, "label-title-dex-name"

    .line 1447
    .line 1448
    invoke-virtual {v3, v4, v6}, Lf/on;->nb1(ILjava/lang/String;)Lf/un0;

    .line 1449
    .line 1450
    .line 1451
    move-result-object v4

    .line 1452
    invoke-virtual {v4}, Lf/un0;->Jr1()V

    .line 1453
    .line 1454
    .line 1455
    invoke-virtual {v3}, Lf/on;->WG()Lf/un0;

    .line 1456
    .line 1457
    .line 1458
    if-eqz v25, :cond_5bb

    .line 1459
    .line 1460
    const/16 v4, 0x193f

    .line 1461
    .line 1462
    move/from16 v6, v29

    .line 1463
    .line 1464
    invoke-static {v2, v4, v7, v6}, Lf/la;->FV0(Lf/lj6;ILjava/util/ArrayList;Z)V

    .line 1465
    .line 1466
    .line 1467
    goto :goto_5bd

    .line 1468
    :cond_5bb
    move/from16 v6, v29

    .line 1469
    .line 1470
    :goto_5bd
    const/16 v4, 0x1940

    .line 1471
    .line 1472
    invoke-static {v2, v4, v8, v6}, Lf/la;->FV0(Lf/lj6;ILjava/util/ArrayList;Z)V

    .line 1473
    .line 1474
    .line 1475
    const/16 v4, 0x193e

    .line 1476
    .line 1477
    invoke-static {v2, v4, v9, v6}, Lf/la;->FV0(Lf/lj6;ILjava/util/ArrayList;Z)V

    .line 1478
    .line 1479
    .line 1480
    const/16 v4, 0x1941

    .line 1481
    .line 1482
    invoke-static {v2, v4, v12, v6}, Lf/la;->FV0(Lf/lj6;ILjava/util/ArrayList;Z)V

    .line 1483
    .line 1484
    .line 1485
    if-eqz v25, :cond_5d3

    .line 1486
    .line 1487
    const/16 v4, 0x1942

    .line 1488
    .line 1489
    invoke-static {v2, v4, v13, v6}, Lf/la;->FV0(Lf/lj6;ILjava/util/ArrayList;Z)V

    .line 1490
    .line 1491
    .line 1492
    :cond_5d3
    const/16 v4, 0x1943

    .line 1493
    .line 1494
    invoke-static {v2, v4, v14, v6}, Lf/la;->FV0(Lf/lj6;ILjava/util/ArrayList;Z)V

    .line 1495
    .line 1496
    .line 1497
    new-instance v4, Lf/rh3;

    .line 1498
    .line 1499
    invoke-direct {v4}, Lf/rh3;-><init>()V

    .line 1500
    .line 1501
    .line 1502
    invoke-virtual {v3, v4}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 1503
    .line 1504
    .line 1505
    move-result-object v3

    .line 1506
    invoke-virtual {v3}, Lf/un0;->Qg0()V

    .line 1507
    .line 1508
    .line 1509
    new-instance v3, Lf/mw0;

    .line 1510
    .line 1511
    invoke-direct {v3, v2}, Lf/mw0;-><init>(Lf/rh3;)V

    .line 1512
    .line 1513
    .line 1514
    const/4 v9, 0x2

    .line 1515
    invoke-virtual {v3, v9}, Lf/mw0;->si1(I)V

    .line 1516
    .line 1517
    .line 1518
    iget-object v2, v1, Lf/lj6;->ir1:Lf/on;

    .line 1519
    .line 1520
    invoke-virtual {v2}, Lf/on;->TN1()Lf/un0;

    .line 1521
    .line 1522
    .line 1523
    move-result-object v4

    .line 1524
    invoke-virtual {v4}, Lf/un0;->Go1()V

    .line 1525
    .line 1526
    .line 1527
    invoke-virtual {v2, v3}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 1528
    .line 1529
    .line 1530
    move-result-object v3

    .line 1531
    invoke-virtual {v3}, Lf/un0;->sY1()V

    .line 1532
    .line 1533
    .line 1534
    invoke-virtual {v2}, Lf/on;->TN1()Lf/un0;

    .line 1535
    .line 1536
    .line 1537
    move-result-object v2

    .line 1538
    invoke-virtual {v2}, Lf/un0;->Go1()V

    .line 1539
    .line 1540
    .line 1541
    invoke-virtual {v10, v1, v0}, Lf/wt3;->Vn0(Lf/rh3;Ljava/lang/String;)Lf/t4;

    .line 1542
    .line 1543
    .line 1544
    const/16 v0, 0x6ef

    .line 1545
    .line 1546
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 1547
    .line 1548
    .line 1549
    move-result-object v0

    .line 1550
    new-instance v1, Lf/wa5;

    .line 1551
    .line 1552
    move-object/from16 v2, p1

    .line 1553
    .line 1554
    move/from16 v6, p3

    .line 1555
    .line 1556
    move-object/from16 v4, v26

    .line 1557
    .line 1558
    invoke-direct {v1, v2, v4, v5, v6}, Lf/wa5;-><init>(Lf/er7;Lf/zp3;BB)V

    .line 1559
    .line 1560
    .line 1561
    invoke-virtual {v10, v1, v0}, Lf/wt3;->Vn0(Lf/rh3;Ljava/lang/String;)Lf/t4;

    .line 1562
    .line 1563
    .line 1564
    const/16 v0, 0x9f6

    .line 1565
    .line 1566
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 1567
    .line 1568
    .line 1569
    move-result-object v1

    .line 1570
    new-instance v3, Lf/z80;

    .line 1571
    .line 1572
    invoke-direct {v3}, Lf/lj6;-><init>()V

    .line 1573
    .line 1574
    .line 1575
    new-instance v5, Lf/v20;

    .line 1576
    .line 1577
    const/16 v7, 0x13

    .line 1578
    .line 1579
    const/4 v12, 0x0

    .line 1580
    invoke-direct {v5, v7, v12}, Lf/v20;-><init>(IZ)V

    .line 1581
    .line 1582
    .line 1583
    new-instance v7, Lf/m17;

    .line 1584
    .line 1585
    invoke-direct {v7}, Lf/pl6;-><init>()V

    .line 1586
    .line 1587
    .line 1588
    iput-object v7, v5, Lf/v20;->xa:Ljava/lang/Object;

    .line 1589
    .line 1590
    new-instance v7, Ljava/util/TreeMap;

    .line 1591
    .line 1592
    invoke-direct {v7}, Ljava/util/TreeMap;-><init>()V

    .line 1593
    .line 1594
    .line 1595
    iput-object v7, v5, Lf/v20;->iL:Ljava/lang/Object;

    .line 1596
    .line 1597
    invoke-virtual {v5, v12, v4}, Lf/v20;->HM(ILf/zp3;)V

    .line 1598
    .line 1599
    .line 1600
    invoke-static {}, Lf/p37;->N91()V

    .line 1601
    .line 1602
    .line 1603
    new-instance v4, Ljava/util/ArrayList;

    .line 1604
    .line 1605
    invoke-direct {v4}, Ljava/util/ArrayList;-><init>()V

    .line 1606
    .line 1607
    .line 1608
    invoke-virtual {v7}, Ljava/util/TreeMap;->values()Ljava/util/Collection;

    .line 1609
    .line 1610
    .line 1611
    move-result-object v7

    .line 1612
    invoke-interface {v7}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    .line 1613
    .line 1614
    .line 1615
    move-result-object v7

    .line 1616
    :goto_64f
    invoke-interface {v7}, Ljava/util/Iterator;->hasNext()Z

    .line 1617
    .line 1618
    .line 1619
    move-result v8

    .line 1620
    const-string v9, "monsterdex-button-evo-tree"

    .line 1621
    .line 1622
    if-eqz v8, :cond_a4b

    .line 1623
    .line 1624
    invoke-interface {v7}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 1625
    .line 1626
    .line 1627
    move-result-object v8

    .line 1628
    check-cast v8, Lf/js8;

    .line 1629
    .line 1630
    iget-object v13, v8, Lf/js8;->d11:Ljava/util/TreeMap;

    .line 1631
    .line 1632
    iget-object v14, v8, Lf/js8;->bd:Ljava/util/TreeMap;

    .line 1633
    .line 1634
    invoke-virtual {v13}, Ljava/util/TreeMap;->values()Ljava/util/Collection;

    .line 1635
    .line 1636
    .line 1637
    move-result-object v13

    .line 1638
    const/4 v0, 0x0

    .line 1639
    new-array v12, v0, [Lf/zp3;

    .line 1640
    .line 1641
    invoke-interface {v13, v12}, Ljava/util/Collection;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;

    .line 1642
    .line 1643
    .line 1644
    move-result-object v12

    .line 1645
    check-cast v12, [Lf/zp3;

    .line 1646
    .line 1647
    array-length v12, v12

    .line 1648
    if-lez v12, :cond_745

    .line 1649
    .line 1650
    iget-object v12, v8, Lf/js8;->d11:Ljava/util/TreeMap;

    .line 1651
    .line 1652
    invoke-virtual {v12}, Ljava/util/TreeMap;->values()Ljava/util/Collection;

    .line 1653
    .line 1654
    .line 1655
    move-result-object v12

    .line 1656
    new-array v13, v0, [Lf/zp3;

    .line 1657
    .line 1658
    invoke-interface {v12, v13}, Ljava/util/Collection;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;

    .line 1659
    .line 1660
    .line 1661
    move-result-object v12

    .line 1662
    check-cast v12, [Lf/zp3;

    .line 1663
    .line 1664
    array-length v13, v12

    .line 1665
    new-array v14, v13, [Lf/ce6;

    .line 1666
    .line 1667
    :goto_682
    if-ge v0, v13, :cond_6a9

    .line 1668
    .line 1669
    move/from16 v26, v0

    .line 1670
    .line 1671
    new-instance v0, Lf/p38;

    .line 1672
    .line 1673
    move-object/from16 v27, v7

    .line 1674
    .line 1675
    aget-object v7, v12, v26

    .line 1676
    .line 1677
    move-object/from16 v28, v12

    .line 1678
    .line 1679
    const/4 v12, 0x0

    .line 1680
    invoke-direct {v0, v2, v7, v12, v6}, Lf/p38;-><init>(Lf/er7;Lf/zp3;BB)V

    .line 1681
    .line 1682
    .line 1683
    aput-object v0, v14, v26

    .line 1684
    .line 1685
    invoke-virtual {v0, v15}, Lf/fq0;->lt0(Ljava/lang/String;)V

    .line 1686
    .line 1687
    .line 1688
    aget-object v0, v14, v26

    .line 1689
    .line 1690
    iget-object v0, v0, Lf/ce6;->bV:Lf/nn2;

    .line 1691
    .line 1692
    const/4 v7, 0x2

    .line 1693
    const/4 v12, 0x5

    .line 1694
    invoke-virtual {v0, v12, v7}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 1695
    .line 1696
    .line 1697
    add-int/lit8 v0, v26, 0x1

    .line 1698
    .line 1699
    move-object/from16 v7, v27

    .line 1700
    .line 1701
    move-object/from16 v12, v28

    .line 1702
    .line 1703
    const/16 v24, 0x5

    .line 1704
    .line 1705
    goto :goto_682

    .line 1706
    :cond_6a9
    move-object/from16 v27, v7

    .line 1707
    .line 1708
    const/4 v12, 0x5

    .line 1709
    iget v0, v8, Lf/js8;->jC1:I

    .line 1710
    .line 1711
    iget-object v7, v5, Lf/v20;->iL:Ljava/lang/Object;

    .line 1712
    .line 1713
    check-cast v7, Ljava/util/TreeMap;

    .line 1714
    .line 1715
    const/16 v16, 0x1

    .line 1716
    .line 1717
    add-int/lit8 v0, v0, 0x1

    .line 1718
    .line 1719
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 1720
    .line 1721
    .line 1722
    move-result-object v0

    .line 1723
    invoke-virtual {v7, v0}, Ljava/util/TreeMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    .line 1724
    .line 1725
    .line 1726
    move-result-object v0

    .line 1727
    check-cast v0, Lf/js8;

    .line 1728
    .line 1729
    if-nez v0, :cond_6c4

    .line 1730
    .line 1731
    :goto_6c2
    const/4 v0, 0x0

    .line 1732
    goto :goto_6e7

    .line 1733
    :cond_6c4
    iget-object v7, v0, Lf/js8;->bd:Ljava/util/TreeMap;

    .line 1734
    .line 1735
    iget-object v0, v0, Lf/js8;->d11:Ljava/util/TreeMap;

    .line 1736
    .line 1737
    invoke-virtual {v0}, Ljava/util/TreeMap;->size()I

    .line 1738
    .line 1739
    .line 1740
    move-result v8

    .line 1741
    invoke-virtual {v7}, Ljava/util/TreeMap;->size()I

    .line 1742
    .line 1743
    .line 1744
    move-result v12

    .line 1745
    invoke-static {v8, v12}, Ljava/lang/Math;->max(II)I

    .line 1746
    .line 1747
    .line 1748
    move-result v8

    .line 1749
    if-nez v8, :cond_6d7

    .line 1750
    .line 1751
    goto :goto_6c2

    .line 1752
    :cond_6d7
    invoke-virtual {v0}, Ljava/util/TreeMap;->size()I

    .line 1753
    .line 1754
    .line 1755
    move-result v0

    .line 1756
    invoke-virtual {v7}, Ljava/util/TreeMap;->size()I

    .line 1757
    .line 1758
    .line 1759
    move-result v7

    .line 1760
    invoke-static {v0, v7}, Ljava/lang/Math;->max(II)I

    .line 1761
    .line 1762
    .line 1763
    move-result v0

    .line 1764
    const/16 v16, 0x1

    .line 1765
    .line 1766
    or-int/lit8 v0, v0, 0x1

    .line 1767
    .line 1768
    :goto_6e7
    or-int/lit8 v7, v13, 0x1

    .line 1769
    .line 1770
    if-le v0, v7, :cond_73d

    .line 1771
    .line 1772
    new-array v7, v0, [Lf/ce6;

    .line 1773
    .line 1774
    const/4 v8, 0x0

    .line 1775
    :goto_6ee
    if-ge v8, v0, :cond_709

    .line 1776
    .line 1777
    new-instance v12, Lf/ce6;

    .line 1778
    .line 1779
    move/from16 v26, v0

    .line 1780
    .line 1781
    const-string v0, "\u2500\u2500"

    .line 1782
    .line 1783
    move-object/from16 v28, v5

    .line 1784
    .line 1785
    const/16 v5, 0x5c

    .line 1786
    .line 1787
    invoke-direct {v12, v0, v5, v5}, Lf/ce6;-><init>(Ljava/lang/String;II)V

    .line 1788
    .line 1789
    .line 1790
    aput-object v12, v7, v8

    .line 1791
    .line 1792
    invoke-virtual {v12, v9}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 1793
    .line 1794
    .line 1795
    add-int/lit8 v8, v8, 0x1

    .line 1796
    .line 1797
    move/from16 v0, v26

    .line 1798
    .line 1799
    move-object/from16 v5, v28

    .line 1800
    .line 1801
    goto :goto_6ee

    .line 1802
    :cond_709
    move/from16 v26, v0

    .line 1803
    .line 1804
    move-object/from16 v28, v5

    .line 1805
    .line 1806
    sub-int v0, v26, v13

    .line 1807
    .line 1808
    div-int/lit8 v5, v0, 0x2

    .line 1809
    .line 1810
    const/4 v12, 0x0

    .line 1811
    invoke-static {v14, v12, v7, v5, v13}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V

    .line 1812
    .line 1813
    .line 1814
    const/4 v9, 0x2

    .line 1815
    if-lt v0, v9, :cond_73b

    .line 1816
    .line 1817
    aget-object v5, v7, v12

    .line 1818
    .line 1819
    const-string v8, "\u250c\u2500\u2500"

    .line 1820
    .line 1821
    invoke-virtual {v5, v8}, Lf/fq0;->lt0(Ljava/lang/String;)V

    .line 1822
    .line 1823
    .line 1824
    add-int/lit8 v5, v26, -0x1

    .line 1825
    .line 1826
    aget-object v5, v7, v5

    .line 1827
    .line 1828
    const-string v8, "\u2514\u2500\u2500"

    .line 1829
    .line 1830
    invoke-virtual {v5, v8}, Lf/fq0;->lt0(Ljava/lang/String;)V

    .line 1831
    .line 1832
    .line 1833
    const/4 v5, 0x4

    .line 1834
    if-lt v0, v5, :cond_73b

    .line 1835
    .line 1836
    const/16 v16, 0x1

    .line 1837
    .line 1838
    aget-object v0, v7, v16

    .line 1839
    .line 1840
    const-string v5, "\u251c\u2500\u2500"

    .line 1841
    .line 1842
    invoke-virtual {v0, v5}, Lf/fq0;->lt0(Ljava/lang/String;)V

    .line 1843
    .line 1844
    .line 1845
    add-int/lit8 v0, v26, -0x2

    .line 1846
    .line 1847
    aget-object v0, v7, v0

    .line 1848
    .line 1849
    invoke-virtual {v0, v5}, Lf/fq0;->lt0(Ljava/lang/String;)V

    .line 1850
    .line 1851
    .line 1852
    :cond_73b
    move-object v14, v7

    .line 1853
    goto :goto_73f

    .line 1854
    :cond_73d
    move-object/from16 v28, v5

    .line 1855
    .line 1856
    :cond_73f
    :goto_73f
    move-object/from16 v13, v22

    .line 1857
    .line 1858
    const/16 v5, 0x9f6

    .line 1859
    .line 1860
    goto/16 :goto_9f6

    .line 1861
    .line 1862
    :cond_745
    move-object/from16 v28, v5

    .line 1863
    .line 1864
    move-object/from16 v27, v7

    .line 1865
    .line 1866
    invoke-virtual {v14}, Ljava/util/TreeMap;->values()Ljava/util/Collection;

    .line 1867
    .line 1868
    .line 1869
    move-result-object v0

    .line 1870
    const/4 v12, 0x0

    .line 1871
    new-array v5, v12, [Lf/b81;

    .line 1872
    .line 1873
    invoke-interface {v0, v5}, Ljava/util/Collection;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;

    .line 1874
    .line 1875
    .line 1876
    move-result-object v0

    .line 1877
    check-cast v0, [Lf/b81;

    .line 1878
    .line 1879
    array-length v0, v0

    .line 1880
    if-lez v0, :cond_a46

    .line 1881
    .line 1882
    invoke-virtual {v14}, Ljava/util/TreeMap;->values()Ljava/util/Collection;

    .line 1883
    .line 1884
    .line 1885
    move-result-object v0

    .line 1886
    new-array v5, v12, [Lf/b81;

    .line 1887
    .line 1888
    invoke-interface {v0, v5}, Ljava/util/Collection;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;

    .line 1889
    .line 1890
    .line 1891
    move-result-object v0

    .line 1892
    check-cast v0, [Lf/b81;

    .line 1893
    .line 1894
    array-length v5, v0

    .line 1895
    new-array v14, v5, [Lf/ce6;

    .line 1896
    .line 1897
    const/4 v7, 0x0

    .line 1898
    :goto_769
    if-ge v7, v5, :cond_73f

    .line 1899
    .line 1900
    new-instance v8, Lf/re7;

    .line 1901
    .line 1902
    aget-object v9, v0, v7

    .line 1903
    .line 1904
    const/16 v12, 0x5c

    .line 1905
    .line 1906
    invoke-direct {v8, v15, v12, v12}, Lf/ce6;-><init>(Ljava/lang/String;II)V

    .line 1907
    .line 1908
    .line 1909
    new-instance v12, Lf/nn2;

    .line 1910
    .line 1911
    invoke-direct {v12, v8}, Lf/nn2;-><init>(Lf/rh3;)V

    .line 1912
    .line 1913
    .line 1914
    iput-object v12, v8, Lf/re7;->rP0:Lf/nn2;

    .line 1915
    .line 1916
    const-string v13, "monsterdex-button"

    .line 1917
    .line 1918
    invoke-virtual {v8, v13}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 1919
    .line 1920
    .line 1921
    const/4 v13, 0x0

    .line 1922
    invoke-virtual {v8, v13}, Lf/fq0;->Ag(Z)V

    .line 1923
    .line 1924
    .line 1925
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 1926
    .line 1927
    .line 1928
    move-result-object v13

    .line 1929
    move-object/from16 v26, v0

    .line 1930
    .line 1931
    iget-short v0, v9, Lf/b81;->P6:S

    .line 1932
    .line 1933
    move/from16 v29, v5

    .line 1934
    .line 1935
    iget-object v5, v9, Lf/b81;->uR0:Lf/vj3;

    .line 1936
    .line 1937
    iget v6, v9, Lf/b81;->zw0:I
    # MonMMO-EX: a data evolution (f/fi7 0x1000) carries its time of day (1 day, 2 night) above the
    # u16 parameter; split it off so the level, item or move label reads the parameter alone.
    move/from16 v35, v6
    ushr-int/lit8 v35, v35, 0x10
    shl-int/lit8 v6, v6, 0x10
    ushr-int/lit8 v6, v6, 0x10

    .line 1938
    .line 1939
    invoke-virtual {v13, v0}, Lf/y91;->wT0(S)Lf/zp3;

    .line 1940
    .line 1941
    .line 1942
    move-result-object v0

    .line 1943
    iget-object v0, v0, Lf/zp3;->U5:Lf/zp3;

    .line 1944
    .line 1945
    sget-object v13, Lf/x74;->Jg0:Lf/x74;

    .line 1946
    .line 1947
    iget-short v9, v9, Lf/b81;->P6:S

    .line 1948
    .line 1949
    invoke-virtual {v2, v13, v9}, Lf/er7;->S02(Lf/x74;S)Z

    .line 1950
    .line 1951
    .line 1952
    move-result v30

    .line 1953
    move/from16 v31, v7

    .line 1954
    .line 1955
    iget-object v7, v8, Lf/ce6;->bV:Lf/nn2;

    .line 1956
    .line 1957
    if-nez v30, :cond_7ca

    .line 1958
    .line 1959
    if-eqz v0, :cond_7b0

    .line 1960
    .line 1961
    iget-short v0, v0, Lf/zp3;->Kj1:S

    .line 1962
    .line 1963
    invoke-virtual {v2, v13, v0}, Lf/er7;->S02(Lf/x74;S)Z

    .line 1964
    .line 1965
    .line 1966
    move-result v0

    .line 1967
    if-nez v0, :cond_7ca

    .line 1968
    .line 1969
    :cond_7b0
    const/4 v12, 0x0

    .line 1970
    invoke-virtual {v8, v12}, Lf/re7;->eU1(S)V

    .line 1971
    .line 1972
    .line 1973
    iget v0, v7, Lf/nn2;->eG:I

    .line 1974
    .line 1975
    iget v5, v7, Lf/nn2;->be1:I

    .line 1976
    .line 1977
    add-int/lit8 v5, v5, 0x6

    .line 1978
    .line 1979
    invoke-virtual {v7, v0, v5}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 1980
    .line 1981
    .line 1982
    const-string v0, "???"

    .line 1983
    .line 1984
    invoke-virtual {v8, v0}, Lf/rh3;->Eq0(Ljava/lang/Object;)V

    .line 1985
    .line 1986
    .line 1987
    iput v12, v8, Lf/rh3;->Lv0:I

    .line 1988
    .line 1989
    move-object/from16 v13, v22

    .line 1990
    .line 1991
    const/16 v5, 0x9f6

    .line 1992
    .line 1993
    goto/16 :goto_9e6

    .line 1994
    .line 1995
    :cond_7ca
    invoke-static {v6, v15}, Lf/yn7;->qo(ILjava/lang/String;)Ljava/lang/String;

    .line 1996
    .line 1997
    .line 1998
    move-result-object v0

    .line 1999
    invoke-virtual {v5}, Ljava/lang/Enum;->ordinal()I

    .line 2000
    .line 2001
    .line 2002
    move-result v13

    .line 2003
    move-object/from16 v30, v0

    .line 2004
    .line 2005
    const/16 v34, 0x3b

    .line 2006
    .line 2007
    packed-switch v13, :pswitch_data_b5e

    .line 2008
    .line 2009
    .line 2010
    const/4 v13, 0x0

    .line 2011
    invoke-virtual {v8, v13}, Lf/re7;->eU1(S)V

    .line 2012
    .line 2013
    .line 2014
    :goto_7dd
    move-object/from16 v13, v22

    .line 2015
    .line 2016
    :goto_7df
    const/4 v9, 0x0

    .line 2017
    goto/16 :goto_99f

    .line 2018
    .line 2019
    :pswitch_7e2
    const/16 v0, 0x1542

    .line 2020
    .line 2021
    invoke-virtual {v8, v0}, Lf/re7;->eU1(S)V

    .line 2022
    .line 2023
    .line 2024
    goto :goto_7dd

    .line 2025
    :pswitch_7e8
    new-instance v9, Ljava/lang/StringBuilder;

    .line 2026
    .line 2027
    invoke-direct {v9}, Ljava/lang/StringBuilder;-><init>()V

    .line 2028
    .line 2029
    .line 2030
    invoke-static/range {v34 .. v34}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 2031
    .line 2032
    .line 2033
    move-result-object v13

    .line 2034
    invoke-virtual {v9, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 2035
    .line 2036
    .line 2037
    move-object/from16 v13, v22

    .line 2038
    .line 2039
    invoke-virtual {v9, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 2040
    .line 2041
    .line 2042
    invoke-virtual {v9, v6}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 2043
    .line 2044
    .line 2045
    invoke-virtual {v9}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 2046
    .line 2047
    .line 2048
    move-result-object v6

    .line 2049
    invoke-virtual {v8, v6}, Lf/fq0;->lt0(Ljava/lang/String;)V

    .line 2050
    .line 2051
    .line 2052
    invoke-static {}, Lf/r41;->a40()Lf/r41;

    .line 2053
    .line 2054
    .line 2055
    move-result-object v6

    .line 2056
    sget-object v9, Lf/vj3;->uM:Lf/vj3;

    .line 2057
    .line 2058
    if-ne v5, v9, :cond_80d

    .line 2059
    .line 2060
    const/4 v9, 0x0

    .line 2061
    goto :goto_80e

    .line 2062
    :cond_80d
    const/4 v9, 0x1

    .line 2063
    :goto_80e
    iget-object v6, v6, Lf/r41;->fW0:[Lf/m39;

    .line 2064
    .line 2065
    aget-object v6, v6, v9

    .line 2066
    .line 2067
    const/4 v9, 0x1

    .line 2068
    new-array v0, v9, [Lf/m39;

    .line 2069
    .line 2070
    const/4 v9, 0x0

    .line 2071
    aput-object v6, v0, v9

    .line 2072
    .line 2073
    invoke-virtual {v12, v0}, Lf/nn2;->vm1([Lf/m39;)Lf/rh3;

    .line 2074
    .line 2075
    .line 2076
    const/4 v0, 0x4

    .line 2077
    const/16 v6, 0x1c

    .line 2078
    .line 2079
    invoke-virtual {v12, v6, v0}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 2080
    .line 2081
    .line 2082
    const/16 v6, 0x13ba

    .line 2083
    .line 2084
    invoke-virtual {v8, v6}, Lf/re7;->eU1(S)V

    .line 2085
    .line 2086
    .line 2087
    goto/16 :goto_99f

    .line 2088
    .line 2089
    :pswitch_828
    move-object/from16 v13, v22

    .line 2090
    .line 2091
    const/16 v0, 0x13ba

    .line 2092
    .line 2093
    const/4 v9, 0x0

    .line 2094
    int-to-short v6, v6

    .line 2095
    invoke-static {v9, v6}, Lf/pr;->Wx(BS)S

    .line 2096
    .line 2097
    .line 2098
    move-result v0

    .line 2099
    sget-object v2, Lf/pr;->Sb1:Lf/pr;

    .line 2100
    .line 2101
    invoke-virtual {v2, v9, v0, v9, v9}, Lf/pr;->IL0(BSZZ)[Lf/wr2;

    .line 2102
    .line 2103
    .line 2104
    move-result-object v0

    .line 2105
    invoke-virtual {v12, v0}, Lf/nn2;->hv0([Lf/wr2;)V

    .line 2106
    .line 2107
    .line 2108
    const/16 v0, 0x13ba

    .line 2109
    .line 2110
    invoke-virtual {v8, v0}, Lf/re7;->eU1(S)V

    .line 2111
    .line 2112
    .line 2113
    const/16 v0, 0x10

    .line 2114
    .line 2115
    const/4 v2, 0x2

    .line 2116
    invoke-virtual {v12, v0, v2}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 2117
    .line 2118
    .line 2119
    const/4 v0, 0x6

    .line 2120
    invoke-virtual {v7, v9, v0}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 2121
    .line 2122
    .line 2123
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 2124
    .line 2125
    .line 2126
    move-result-object v0

    .line 2127
    invoke-virtual {v0, v6}, Lf/y91;->wT0(S)Lf/zp3;

    .line 2128
    .line 2129
    .line 2130
    move-result-object v0

    .line 2131
    :goto_852
    invoke-virtual {v0, v9}, Lf/zp3;->xt(Z)Ljava/lang/String;

    .line 2132
    .line 2133
    .line 2134
    move-result-object v0

    .line 2135
    goto/16 :goto_9a1

    .line 2136
    .line 2137
    :pswitch_858
    move-object/from16 v13, v22

    .line 2138
    .line 2139
    const/4 v9, 0x0

    .line 2140
    sget-object v0, Lf/an8;->LU:Lf/an8;

    .line 2141
    .line 2142
    const/16 v2, 0x14d4

    .line 2143
    .line 2144
    invoke-virtual {v0, v2}, Lf/an8;->R3(S)Lf/ls0;

    .line 2145
    .line 2146
    .line 2147
    move-result-object v0

    .line 2148
    sget-object v2, Lf/c21;->WT1:Lf/c21;

    .line 2149
    .line 2150
    invoke-virtual {v2, v0, v9}, Lf/c21;->co1(Lf/ls0;Z)Lf/qj6;

    .line 2151
    .line 2152
    .line 2153
    move-result-object v0

    .line 2154
    const/4 v2, 0x1

    .line 2155
    const/16 v17, 0x0

    .line 2156
    .line 2157
    new-array v9, v2, [Lf/qj6;

    .line 2158
    .line 2159
    aput-object v0, v9, v17

    .line 2160
    .line 2161
    invoke-virtual {v12, v9}, Lf/nn2;->zC0([Lf/qj6;)Lf/rh3;

    .line 2162
    .line 2163
    .line 2164
    const/16 v0, 0x18

    .line 2165
    .line 2166
    invoke-virtual {v12, v0, v0}, Lf/nn2;->Wa(II)V

    .line 2167
    .line 2168
    .line 2169
    const/16 v0, 0x13ba

    .line 2170
    .line 2171
    invoke-virtual {v8, v0}, Lf/re7;->eU1(S)V

    .line 2172
    .line 2173
    .line 2174
    const/16 v0, 0xa

    .line 2175
    .line 2176
    const/16 v2, 0x14

    .line 2177
    .line 2178
    invoke-virtual {v12, v2, v0}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 2179
    .line 2180
    .line 2181
    const/4 v0, 0x6

    .line 2182
    const/4 v9, 0x0

    .line 2183
    invoke-virtual {v7, v9, v0}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 2184
    .line 2185
    .line 2186
    invoke-static {}, Lf/k92;->dh0()Lf/k92;

    .line 2187
    .line 2188
    .line 2189
    move-result-object v0

    .line 2190
    int-to-short v2, v6

    .line 2191
    invoke-virtual {v0, v2}, Lf/k92;->BW1(S)Lf/hu6;

    .line 2192
    .line 2193
    .line 2194
    move-result-object v0

    .line 2195
    iget v0, v0, Lf/hu6;->bl1:I

    .line 2196
    .line 2197
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 2198
    .line 2199
    .line 2200
    move-result-object v0

    .line 2201
    :goto_898
    const/4 v9, 0x0

    .line 2202
    goto/16 :goto_9a1

    .line 2203
    .line 2204
    :pswitch_89b
    move-object/from16 v13, v22

    .line 2205
    .line 2206
    sget-object v0, Lf/vj3;->Hd1:Lf/vj3;

    .line 2207
    .line 2208
    if-ne v5, v0, :cond_8a9

    .line 2209
    .line 2210
    invoke-static {}, Lf/r41;->a40()Lf/r41;

    .line 2211
    .line 2212
    .line 2213
    move-result-object v0

    .line 2214
    iget-object v0, v0, Lf/r41;->sW1:Lf/m39;

    .line 2215
    .line 2216
    :goto_8a7
    const/4 v2, 0x1

    .line 2217
    goto :goto_8b0

    .line 2218
    :cond_8a9
    invoke-static {}, Lf/r41;->a40()Lf/r41;

    .line 2219
    .line 2220
    .line 2221
    move-result-object v0

    .line 2222
    iget-object v0, v0, Lf/r41;->xr0:Lf/m39;

    .line 2223
    .line 2224
    goto :goto_8a7

    .line 2225
    :goto_8b0
    new-array v9, v2, [Lf/m39;

    .line 2226
    .line 2227
    const/4 v2, 0x0

    .line 2228
    aput-object v0, v9, v2

    .line 2229
    .line 2230
    invoke-virtual {v12, v9}, Lf/nn2;->vm1([Lf/m39;)Lf/rh3;

    .line 2231
    .line 2232
    .line 2233
    int-to-short v0, v6

    .line 2234
    invoke-virtual {v8, v0}, Lf/re7;->eU1(S)V

    .line 2235
    .line 2236
    .line 2237
    const/16 v6, 0xa

    .line 2238
    .line 2239
    const/16 v9, 0x14

    .line 2240
    .line 2241
    invoke-virtual {v12, v9, v6}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 2242
    .line 2243
    .line 2244
    const/4 v6, 0x6

    .line 2245
    invoke-virtual {v7, v2, v6}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 2246
    .line 2247
    .line 2248
    :goto_8c7
    sget-object v2, Lf/an8;->LU:Lf/an8;

    .line 2249
    .line 2250
    invoke-virtual {v2, v0}, Lf/an8;->R3(S)Lf/ls0;

    .line 2251
    .line 2252
    .line 2253
    move-result-object v0

    .line 2254
    iget v0, v0, Lf/ls0;->FU:I

    .line 2255
    .line 2256
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 2257
    .line 2258
    .line 2259
    move-result-object v0

    .line 2260
    goto :goto_898

    .line 2261
    :pswitch_8d4
    move-object/from16 v13, v22

    .line 2262
    .line 2263
    invoke-static {}, Lf/r41;->a40()Lf/r41;

    .line 2264
    .line 2265
    .line 2266
    move-result-object v0

    .line 2267
    sget-object v2, Lf/vj3;->Iu0:Lf/vj3;

    .line 2268
    .line 2269
    if-ne v5, v2, :cond_8e0

    .line 2270
    .line 2271
    const/4 v2, 0x0

    .line 2272
    goto :goto_8e1

    .line 2273
    :cond_8e0
    const/4 v2, 0x1

    .line 2274
    :goto_8e1
    iget-object v0, v0, Lf/r41;->fW0:[Lf/m39;

    .line 2275
    .line 2276
    aget-object v0, v0, v2

    .line 2277
    .line 2278
    const/4 v2, 0x1

    .line 2279
    new-array v9, v2, [Lf/m39;

    .line 2280
    .line 2281
    const/16 v17, 0x0

    .line 2282
    .line 2283
    aput-object v0, v9, v17

    .line 2284
    .line 2285
    invoke-virtual {v12, v9}, Lf/nn2;->vm1([Lf/m39;)Lf/rh3;

    .line 2286
    .line 2287
    .line 2288
    const/16 v0, 0x1a

    .line 2289
    .line 2290
    const/16 v2, 0x1c

    .line 2291
    .line 2292
    invoke-virtual {v12, v2, v0}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 2293
    .line 2294
    .line 2295
    :goto_8f6
    int-to-short v0, v6

    .line 2296
    invoke-virtual {v8, v0}, Lf/re7;->eU1(S)V

    .line 2297
    .line 2298
    .line 2299
    goto :goto_8c7

    .line 2300
    :pswitch_8fb
    move-object/from16 v13, v22

    .line 2301
    .line 2302
    const/16 v0, 0x148d

    .line 2303
    .line 2304
    :goto_8ff
    invoke-virtual {v8, v0}, Lf/re7;->eU1(S)V

    .line 2305
    .line 2306
    .line 2307
    goto/16 :goto_7df

    .line 2308
    .line 2309
    :pswitch_904
    move-object/from16 v13, v22

    .line 2310
    .line 2311
    const/16 v0, 0x138c

    .line 2312
    .line 2313
    goto :goto_8ff

    .line 2314
    :pswitch_909
    move-object/from16 v13, v22

    .line 2315
    .line 2316
    const/4 v9, 0x0

    .line 2317
    invoke-virtual {v8, v9}, Lf/re7;->eU1(S)V

    .line 2318
    .line 2319
    .line 2320
    new-instance v0, Ljava/lang/StringBuilder;

    .line 2321
    .line 2322
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    .line 2323
    .line 2324
    .line 2325
    :goto_914
    invoke-static/range {v34 .. v34}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 2326
    .line 2327
    .line 2328
    move-result-object v2

    .line 2329
    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 2330
    .line 2331
    .line 2332
    invoke-virtual {v0, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 2333
    .line 2334
    .line 2335
    invoke-virtual {v0, v6}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 2336
    .line 2337
    .line 2338
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 2339
    .line 2340
    .line 2341
    move-result-object v0

    .line 2342
    invoke-virtual {v8, v0}, Lf/fq0;->lt0(Ljava/lang/String;)V

    .line 2343
    .line 2344
    .line 2345
    goto/16 :goto_7df

    .line 2346
    .line 2347
    :pswitch_92a
    move-object/from16 v13, v22

    .line 2348
    .line 2349
    const/16 v0, 0x13c2

    .line 2350
    .line 2351
    goto :goto_8ff

    .line 2352
    :pswitch_92f
    move-object/from16 v13, v22

    .line 2353
    .line 2354
    const/16 v0, 0x13c0

    .line 2355
    .line 2356
    goto :goto_8ff

    .line 2357
    :pswitch_934
    move-object/from16 v13, v22

    .line 2358
    .line 2359
    const/16 v0, 0x13c1

    .line 2360
    .line 2361
    goto :goto_8ff

    .line 2362
    :pswitch_939
    move-object/from16 v13, v22

    .line 2363
    .line 2364
    const/16 v0, 0x153d

    .line 2365
    .line 2366
    invoke-virtual {v8, v0}, Lf/re7;->eU1(S)V

    .line 2367
    .line 2368
    .line 2369
    const/16 v0, 0x269

    .line 2370
    .line 2371
    if-ne v9, v0, :cond_947

    .line 2372
    .line 2373
    const/16 v0, 0x24c

    .line 2374
    .line 2375
    goto :goto_949

    .line 2376
    :cond_947
    const/16 v0, 0x268

    .line 2377
    .line 2378
    :goto_949
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 2379
    .line 2380
    .line 2381
    move-result-object v2

    .line 2382
    invoke-virtual {v2, v0}, Lf/y91;->wT0(S)Lf/zp3;

    .line 2383
    .line 2384
    .line 2385
    move-result-object v0

    .line 2386
    const/4 v9, 0x0

    .line 2387
    goto/16 :goto_852

    .line 2388
    .line 2389
    :pswitch_954
    move-object/from16 v13, v22

    .line 2390
    .line 2391
    goto :goto_8f6

    .line 2392
    :pswitch_957
    move-object/from16 v13, v22

    .line 2393
    .line 2394
    const/16 v0, 0x153d

    .line 2395
    .line 2396
    goto :goto_8ff

    .line 2397
    :pswitch_95c
    move-object/from16 v13, v22

    .line 2398
    .line 2399
    const/16 v0, 0x13ba

    .line 2400
    .line 2401
    invoke-virtual {v8, v0}, Lf/re7;->eU1(S)V

    .line 2402
    .line 2403
    .line 2404
    new-instance v0, Ljava/lang/StringBuilder;

    .line 2405
    .line 2406
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    .line 2407
    .line 2408
    .line 2409
    goto :goto_914

    .line 2410
    :pswitch_969
    move-object/from16 v13, v22

    .line 2411
    .line 2412
    sget-object v0, Lf/vj3;->ns0:Lf/vj3;

    .line 2413
    .line 2414
    if-ne v5, v0, :cond_977

    .line 2415
    .line 2416
    invoke-static {}, Lf/r41;->a40()Lf/r41;

    .line 2417
    .line 2418
    .line 2419
    move-result-object v0

    .line 2420
    iget-object v0, v0, Lf/r41;->sW1:Lf/m39;

    .line 2421
    .line 2422
    :goto_975
    const/4 v2, 0x1

    .line 2423
    goto :goto_97e

    .line 2424
    :cond_977
    invoke-static {}, Lf/r41;->a40()Lf/r41;

    .line 2425
    .line 2426
    .line 2427
    move-result-object v0

    .line 2428
    iget-object v0, v0, Lf/r41;->xr0:Lf/m39;

    .line 2429
    .line 2430
    goto :goto_975

    .line 2431
    :goto_97e
    new-array v6, v2, [Lf/m39;

    .line 2432
    .line 2433
    const/4 v9, 0x0

    .line 2434
    aput-object v0, v6, v9

    .line 2435
    .line 2436
    invoke-virtual {v12, v6}, Lf/nn2;->vm1([Lf/m39;)Lf/rh3;

    .line 2437
    .line 2438
    .line 2439
    const/16 v0, 0x1462

    .line 2440
    .line 2441
    invoke-virtual {v8, v0}, Lf/re7;->eU1(S)V

    .line 2442
    .line 2443
    .line 2444
    const/16 v0, 0xa

    .line 2445
    .line 2446
    const/16 v2, 0x14

    .line 2447
    .line 2448
    invoke-virtual {v12, v2, v0}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 2449
    .line 2450
    .line 2451
    const/4 v0, 0x6

    .line 2452
    invoke-virtual {v7, v9, v0}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 2453
    .line 2454
    .line 2455
    goto :goto_99f

    .line 2456
    :pswitch_997
    move-object/from16 v13, v22

    .line 2457
    .line 2458
    const/16 v0, 0x1462

    .line 2459
    .line 2460
    const/4 v9, 0x0

    .line 2461
    invoke-virtual {v8, v0}, Lf/re7;->eU1(S)V

    .line 2462
    .line 2463
    .line 2464
    :goto_99f
    move-object/from16 v0, v30

    .line 2465
    .line 2466
    :goto_9a1
    # MonMMO-EX: every label case joins here. A data evolution's badge code (f/fi7 0x1000, the byte
    # above the parameter) gets its symbol at the friendship and held-item cases' badge offsets:
    # 1 day (sun sW1), 2 night (moon xr0), 3 Mega Evolution, 4 alpha (Primal Kyogre), 5 omega
    # (Primal Groudon). v2 and v6 are scratch here: both are written below before they are read
    # again (v5 is reloaded from v29 at :goto_9e6).
    move/from16 v6, v35
    if-eqz v6, :mmx_time_badge_done
    invoke-static {}, Lf/r41;->a40()Lf/r41;
    move-result-object v2
    packed-switch v6, :mmx_badge_switch
    goto :mmx_time_badge_done

    :mmx_badge_switch
    .packed-switch 0x1
        :mmx_badge_sun
        :mmx_badge_moon
        :mmx_badge_mega
        :mmx_badge_alpha
        :mmx_badge_omega
    .end packed-switch

    :mmx_badge_sun
    iget-object v2, v2, Lf/r41;->sW1:Lf/m39;
    goto :mmx_time_badge_draw

    :mmx_badge_moon
    iget-object v2, v2, Lf/r41;->xr0:Lf/m39;
    goto :mmx_time_badge_draw

    :mmx_badge_mega
    iget-object v2, v2, Lf/r41;->mmxMega:Lf/m39;
    goto :mmx_time_badge_draw

    :mmx_badge_alpha
    iget-object v2, v2, Lf/r41;->mmxAlpha:Lf/m39;
    goto :mmx_time_badge_draw

    :mmx_badge_omega
    iget-object v2, v2, Lf/r41;->mmxOmega:Lf/m39;

    :mmx_time_badge_draw
    filled-new-array {v2}, [Lf/m39;
    move-result-object v2
    invoke-virtual {v12, v2}, Lf/nn2;->vm1([Lf/m39;)Lf/rh3;
    const/16 v2, 0x14
    const/16 v6, 0xa
    invoke-virtual {v12, v2, v6}, Lf/nn2;->YG0(II)Lf/rh3;
    const/4 v2, 0x0
    const/4 v6, 0x6
    invoke-virtual {v7, v2, v6}, Lf/nn2;->YG0(II)Lf/rh3;

    :mmx_time_badge_done
    iget-byte v2, v5, Lf/vj3;->v9:B

    .line 2467
    .line 2468
    const/16 v5, 0x9f6

    .line 2469
    .line 2470
    add-int/2addr v2, v5

    # MonMMO-EX: a Mega or Primal entry (badge 3-5, method 0) is labelled by its own string instead
    # of method 0's: 2578 "Mega Evolution", 2579 "Primal Reversion".
    move/from16 v6, v35
    add-int/lit8 v6, v6, -0x3
    if-ltz v6, :mmx_form_label_done
    const/16 v2, 0xa12
    if-eqz v6, :mmx_form_label_done
    const/16 v2, 0xa13

    :mmx_form_label_done

    .line 2471
    invoke-static {v2, v0}, Lf/gt0;->RZ(ILjava/lang/String;)Ljava/lang/String;

    .line 2472
    .line 2473
    .line 2474
    move-result-object v0

    .line 2475
    invoke-virtual {v8, v0}, Lf/rh3;->Eq0(Ljava/lang/Object;)V

    .line 2476
    .line 2477
    .line 2478
    iput v9, v8, Lf/rh3;->Lv0:I

    .line 2479
    .line 2480
    invoke-static {}, Lf/p37;->N91()V

    .line 2481
    .line 2482
    .line 2483
    const/high16 v0, 0x40000000    # 2.0f

    .line 2484
    .line 2485
    iput v0, v7, Lf/nn2;->BE:F

    .line 2486
    .line 2487
    iget v2, v7, Lf/nn2;->eG:I

    .line 2488
    .line 2489
    const/16 v23, 0x2

    .line 2490
    .line 2491
    mul-int/lit8 v2, v2, 0x2

    .line 2492
    .line 2493
    iget v6, v7, Lf/nn2;->be1:I

    .line 2494
    .line 2495
    mul-int/lit8 v6, v6, 0x2

    .line 2496
    .line 2497
    invoke-virtual {v7, v2, v6}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 2498
    .line 2499
    .line 2500
    iput v0, v12, Lf/nn2;->BE:F

    .line 2501
    .line 2502
    iget v0, v12, Lf/nn2;->eG:I

    .line 2503
    .line 2504
    mul-int/lit8 v0, v0, 0x2

    .line 2505
    .line 2506
    iget v2, v12, Lf/nn2;->be1:I

    .line 2507
    .line 2508
    mul-int/lit8 v2, v2, 0x2

    .line 2509
    .line 2510
    invoke-virtual {v12, v0, v2}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 2511
    .line 2512
    .line 2513
    iget-object v0, v8, Lf/fq0;->eH0:Ljava/lang/String;

    .line 2514
    .line 2515
    invoke-virtual {v0, v15}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    .line 2516
    .line 2517
    .line 2518
    move-result v0

    .line 2519
    if-eqz v0, :cond_9e4

    .line 2520
    .line 2521
    iget v0, v7, Lf/nn2;->eG:I

    .line 2522
    .line 2523
    iget v2, v7, Lf/nn2;->be1:I

    .line 2524
    .line 2525
    const/16 v18, 0x6

    .line 2526
    .line 2527
    add-int/lit8 v2, v2, 0x6

    .line 2528
    .line 2529
    invoke-virtual {v7, v0, v2}, Lf/nn2;->YG0(II)Lf/rh3;

    .line 2530
    .line 2531
    .line 2532
    goto :goto_9e6

    .line 2533
    :cond_9e4
    const/16 v18, 0x6

    .line 2534
    .line 2535
    :goto_9e6
    aput-object v8, v14, v31

    .line 2536
    .line 2537
    add-int/lit8 v7, v31, 0x1

    .line 2538
    .line 2539
    move-object/from16 v2, p1

    .line 2540
    .line 2541
    move/from16 v6, p3

    .line 2542
    .line 2543
    move-object/from16 v22, v13

    .line 2544
    .line 2545
    move-object/from16 v0, v26

    .line 2546
    .line 2547
    move/from16 v5, v29

    .line 2548
    .line 2549
    goto/16 :goto_769

    .line 2550
    .line 2551
    :goto_9f6
    array-length v0, v14

    .line 2552
    if-lez v0, :cond_a31

    .line 2553
    .line 2554
    array-length v0, v14

    .line 2555
    const/16 v23, 0x2

    .line 2556
    .line 2557
    rem-int/lit8 v0, v0, 0x2

    .line 2558
    .line 2559
    if-nez v0, :cond_a31

    .line 2560
    .line 2561
    array-length v0, v14

    .line 2562
    const/16 v16, 0x1

    .line 2563
    .line 2564
    add-int/lit8 v0, v0, 0x1

    .line 2565
    .line 2566
    new-array v0, v0, [Lf/ce6;

    .line 2567
    .line 2568
    const/4 v2, 0x0

    .line 2569
    :goto_a08
    array-length v6, v14

    .line 2570
    if-ge v2, v6, :cond_a1b

    .line 2571
    .line 2572
    array-length v6, v14

    .line 2573
    div-int/lit8 v6, v6, 0x2

    .line 2574
    .line 2575
    if-lt v2, v6, :cond_a12

    .line 2576
    .line 2577
    const/4 v6, 0x1

    .line 2578
    goto :goto_a13

    .line 2579
    :cond_a12
    const/4 v6, 0x0

    .line 2580
    :goto_a13
    add-int/2addr v6, v2

    .line 2581
    aget-object v7, v14, v2

    .line 2582
    .line 2583
    aput-object v7, v0, v6

    .line 2584
    .line 2585
    add-int/lit8 v2, v2, 0x1

    .line 2586
    .line 2587
    goto :goto_a08

    .line 2588
    :cond_a1b
    array-length v2, v14

    .line 2589
    div-int/lit8 v2, v2, 0x2

    .line 2590
    .line 2591
    new-instance v6, Lf/ce6;

    .line 2592
    .line 2593
    const/16 v12, 0x5c

    .line 2594
    .line 2595
    invoke-direct {v6, v15, v12, v12}, Lf/ce6;-><init>(Ljava/lang/String;II)V

    .line 2596
    .line 2597
    .line 2598
    aput-object v6, v0, v2

    .line 2599
    .line 2600
    array-length v2, v14

    .line 2601
    div-int/lit8 v2, v2, 0x2

    .line 2602
    .line 2603
    aget-object v2, v0, v2

    .line 2604
    .line 2605
    const/4 v12, 0x0

    .line 2606
    invoke-virtual {v2, v12}, Lf/fq0;->UO(Z)V

    .line 2607
    .line 2608
    .line 2609
    move-object v14, v0

    .line 2610
    :cond_a31
    invoke-virtual {v4, v14}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 2611
    .line 2612
    .line 2613
    move-object/from16 v2, p1

    .line 2614
    .line 2615
    move/from16 v6, p3

    .line 2616
    .line 2617
    move-object/from16 v22, v13

    .line 2618
    .line 2619
    :goto_a3a
    move-object/from16 v7, v27

    .line 2620
    .line 2621
    move-object/from16 v5, v28

    .line 2622
    .line 2623
    const/16 v0, 0x9f6

    .line 2624
    .line 2625
    const/16 v19, 0x4

    .line 2626
    .line 2627
    const/16 v24, 0x5

    .line 2628
    .line 2629
    goto/16 :goto_64f

    .line 2630
    .line 2631
    :cond_a46
    move-object/from16 v2, p1

    .line 2632
    .line 2633
    move/from16 v6, p3

    .line 2634
    .line 2635
    goto :goto_a3a

    .line 2636
    :cond_a4b
    invoke-virtual {v4}, Ljava/util/ArrayList;->size()I

    .line 2637
    .line 2638
    .line 2639
    move-result v0

    .line 2640
    const/4 v2, 0x0

    .line 2641
    const/4 v5, 0x0

    .line 2642
    :cond_a51
    :goto_a51
    if-ge v5, v0, :cond_a60

    .line 2643
    .line 2644
    invoke-virtual {v4, v5}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 2645
    .line 2646
    .line 2647
    move-result-object v6

    .line 2648
    add-int/lit8 v5, v5, 0x1

    .line 2649
    .line 2650
    check-cast v6, [Lf/ce6;

    .line 2651
    .line 2652
    array-length v7, v6

    .line 2653
    if-le v7, v2, :cond_a51

    .line 2654
    .line 2655
    array-length v2, v6

    .line 2656
    goto :goto_a51

    .line 2657
    :cond_a60
    new-instance v0, Lf/lj6;

    .line 2658
    .line 2659
    invoke-direct {v0}, Lf/lj6;-><init>()V

    .line 2660
    .line 2661
    .line 2662
    invoke-virtual {v0, v11}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 2663
    .line 2664
    .line 2665
    const/4 v12, 0x1

    .line 2666
    iput-boolean v12, v0, Lf/lj6;->vn1:Z

    .line 2667
    .line 2668
    iget-object v5, v0, Lf/lj6;->ir1:Lf/on;

    .line 2669
    .line 2670
    iget-object v6, v5, Lf/on;->I91:Lf/un0;

    .line 2671
    .line 2672
    const/high16 v8, 0x40a00000    # 5.0f

    .line 2673
    .line 2674
    invoke-virtual {v6, v8}, Lf/un0;->q71(F)V

    .line 2675
    .line 2676
    .line 2677
    const/4 v11, 0x0

    .line 2678
    :goto_a75
    if-ge v11, v2, :cond_b18

    .line 2679
    .line 2680
    invoke-virtual {v5}, Lf/on;->TN1()Lf/un0;

    .line 2681
    .line 2682
    .line 2683
    move-result-object v6

    .line 2684
    invoke-virtual {v6}, Lf/un0;->Go1()V

    .line 2685
    .line 2686
    .line 2687
    const/4 v6, 0x0

    .line 2688
    :goto_a7f
    invoke-virtual {v4}, Ljava/util/ArrayList;->size()I

    .line 2689
    .line 2690
    .line 2691
    move-result v7

    .line 2692
    if-ge v6, v7, :cond_b05

    .line 2693
    .line 2694
    invoke-virtual {v4, v6}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 2695
    .line 2696
    .line 2697
    move-result-object v7

    .line 2698
    check-cast v7, [Lf/ce6;

    .line 2699
    .line 2700
    array-length v8, v7

    .line 2701
    sub-int v8, v2, v8

    .line 2702
    .line 2703
    const/16 v23, 0x2

    .line 2704
    .line 2705
    div-int/lit8 v8, v8, 0x2

    .line 2706
    .line 2707
    if-lt v11, v8, :cond_aa2

    .line 2708
    .line 2709
    array-length v13, v7

    .line 2710
    add-int/2addr v13, v8

    .line 2711
    if-ge v11, v13, :cond_aa2

    .line 2712
    .line 2713
    sub-int v13, v11, v8

    .line 2714
    .line 2715
    aget-object v13, v7, v13

    .line 2716
    .line 2717
    iget-boolean v13, v13, Lf/rh3;->jI1:Z

    .line 2718
    .line 2719
    if-eqz v13, :cond_aa2

    .line 2720
    .line 2721
    const/4 v13, 0x1

    .line 2722
    goto :goto_aa3

    .line 2723
    :cond_aa2
    const/4 v13, 0x0

    .line 2724
    :goto_aa3
    if-lez v6, :cond_ae3

    .line 2725
    .line 2726
    add-int/lit8 v15, v6, -0x1

    .line 2727
    .line 2728
    invoke-virtual {v4, v15}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 2729
    .line 2730
    .line 2731
    move-result-object v15

    .line 2732
    check-cast v15, [Lf/ce6;

    .line 2733
    .line 2734
    array-length v12, v15

    .line 2735
    sub-int v12, v2, v12

    .line 2736
    .line 2737
    const/16 v23, 0x2

    .line 2738
    .line 2739
    div-int/lit8 v12, v12, 0x2

    .line 2740
    .line 2741
    if-lt v11, v12, :cond_ada

    .line 2742
    .line 2743
    array-length v14, v15

    .line 2744
    add-int/2addr v14, v12

    .line 2745
    if-ge v11, v14, :cond_ada

    .line 2746
    .line 2747
    sub-int v12, v11, v12

    .line 2748
    .line 2749
    aget-object v12, v15, v12

    .line 2750
    .line 2751
    iget-boolean v12, v12, Lf/rh3;->jI1:Z

    .line 2752
    .line 2753
    if-eqz v12, :cond_ada

    .line 2754
    .line 2755
    if-eqz v13, :cond_ada

    .line 2756
    .line 2757
    new-instance v12, Lf/ce6;

    .line 2758
    .line 2759
    const-string v14, "\u2500\u27a4"

    .line 2760
    .line 2761
    const/16 v15, 0x5c

    .line 2762
    .line 2763
    invoke-direct {v12, v14, v15, v15}, Lf/ce6;-><init>(Ljava/lang/String;II)V

    .line 2764
    .line 2765
    .line 2766
    invoke-virtual {v12, v9}, Lf/rh3;->DO1(Ljava/lang/String;)V

    .line 2767
    .line 2768
    .line 2769
    invoke-virtual {v5, v12}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 2770
    .line 2771
    .line 2772
    move-result-object v12

    .line 2773
    const/high16 v14, 0x42b80000    # 92.0f

    .line 2774
    .line 2775
    :goto_ad6
    invoke-virtual {v12, v14, v14}, Lf/un0;->l21(FF)V

    .line 2776
    .line 2777
    .line 2778
    goto :goto_ae9

    .line 2779
    :cond_ada
    const/high16 v14, 0x42b80000    # 92.0f

    .line 2780
    .line 2781
    const/16 v15, 0x5c

    .line 2782
    .line 2783
    invoke-virtual {v5}, Lf/on;->TN1()Lf/un0;

    .line 2784
    .line 2785
    .line 2786
    move-result-object v12

    .line 2787
    goto :goto_ad6

    .line 2788
    :cond_ae3
    const/high16 v14, 0x42b80000    # 92.0f

    .line 2789
    .line 2790
    const/16 v15, 0x5c

    .line 2791
    .line 2792
    const/16 v23, 0x2

    .line 2793
    .line 2794
    :goto_ae9
    if-eqz v13, :cond_af7

    .line 2795
    .line 2796
    sub-int v8, v11, v8

    .line 2797
    .line 2798
    aget-object v7, v7, v8

    .line 2799
    .line 2800
    invoke-virtual {v5, v7}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 2801
    .line 2802
    .line 2803
    move-result-object v7

    .line 2804
    :goto_af3
    invoke-virtual {v7, v14, v14}, Lf/un0;->l21(FF)V

    .line 2805
    .line 2806
    .line 2807
    goto :goto_b00

    .line 2808
    :cond_af7
    if-lt v11, v8, :cond_afb

    .line 2809
    .line 2810
    array-length v7, v7

    .line 2811
    add-int/2addr v8, v7

    .line 2812
    :cond_afb
    invoke-virtual {v5}, Lf/on;->TN1()Lf/un0;

    .line 2813
    .line 2814
    .line 2815
    move-result-object v7

    .line 2816
    goto :goto_af3

    .line 2817
    :goto_b00
    add-int/lit8 v6, v6, 0x1

    .line 2818
    .line 2819
    const/4 v12, 0x1

    .line 2820
    goto/16 :goto_a7f

    .line 2821
    .line 2822
    :cond_b05
    const/16 v15, 0x5c

    .line 2823
    .line 2824
    const/16 v23, 0x2

    .line 2825
    .line 2826
    invoke-virtual {v5}, Lf/on;->TN1()Lf/un0;

    .line 2827
    .line 2828
    .line 2829
    move-result-object v6

    .line 2830
    invoke-virtual {v6}, Lf/un0;->Go1()V

    .line 2831
    .line 2832
    .line 2833
    invoke-virtual {v0}, Lf/lj6;->ub()Lf/un0;

    .line 2834
    .line 2835
    .line 2836
    add-int/lit8 v11, v11, 0x1

    .line 2837
    .line 2838
    const/4 v12, 0x1

    .line 2839
    goto/16 :goto_a75

    .line 2840
    .line 2841
    :cond_b18
    new-instance v2, Lf/mw0;

    .line 2842
    .line 2843
    const/4 v9, 0x0

    .line 2844
    invoke-direct {v2, v9}, Lf/mw0;-><init>(Lf/rh3;)V

    .line 2845
    .line 2846
    .line 2847
    invoke-virtual {v2, v0}, Lf/mw0;->Yf1(Lf/rh3;)V

    .line 2848
    .line 2849
    .line 2850
    invoke-virtual {v2}, Lf/mw0;->fa()V

    .line 2851
    .line 2852
    .line 2853
    iget-object v0, v3, Lf/lj6;->ir1:Lf/on;

    .line 2854
    .line 2855
    invoke-virtual {v0, v2}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 2856
    .line 2857
    .line 2858
    move-result-object v0

    .line 2859
    invoke-virtual {v0}, Lf/un0;->sY1()V

    .line 2860
    .line 2861
    .line 2862
    invoke-virtual {v10, v3, v1}, Lf/wt3;->Vn0(Lf/rh3;Ljava/lang/String;)Lf/t4;

    .line 2863
    .line 2864
    .line 2865
    const/4 v12, 0x0

    .line 2866
    invoke-virtual {v10, v12}, Lf/wt3;->MV(I)V

    .line 2867
    .line 2868
    .line 2869
    move-object/from16 v0, v21

    .line 2870
    .line 2871
    iget-object v0, v0, Lf/lj6;->ir1:Lf/on;

    .line 2872
    .line 2873
    invoke-virtual {v0, v10}, Lf/on;->kw(Ljava/lang/Object;)Lf/un0;

    .line 2874
    .line 2875
    .line 2876
    move-result-object v0

    .line 2877
    invoke-virtual {v0}, Lf/un0;->sY1()V

    .line 2878
    .line 2879
    .line 2880
    new-instance v0, Lf/z91;

    .line 2881
    .line 2882
    move-object/from16 v1, p0

    .line 2883
    .line 2884
    invoke-direct {v0, v1}, Lf/z91;-><init>(Lf/rh3;)V

    .line 2885
    .line 2886
    .line 2887
    invoke-virtual {v0, v10}, Lf/z91;->cU1(Lf/rh3;)V

    .line 2888
    .line 2889
    .line 2890
    invoke-virtual {v0, v10}, Lf/z91;->rZ1(Lf/wt3;)V

    .line 2891
    .line 2892
    .line 2893
    new-instance v2, Lf/b6;

    .line 2894
    .line 2895
    const/16 v3, 0xd

    .line 2896
    .line 2897
    invoke-direct {v2, v3, v1}, Lf/b6;-><init>(ILjava/lang/Object;)V

    .line 2898
    .line 2899
    .line 2900
    invoke-virtual {v0, v2}, Lf/z91;->QQ1(Ljava/lang/Runnable;)V

    .line 2901
    .line 2902
    .line 2903
    invoke-virtual {v1, v0}, Lf/rh3;->Yh1(Lf/z91;)V

    .line 2904
    .line 2905
    .line 2906
    invoke-virtual {v0}, Lf/z91;->F5()V

    .line 2907
    .line 2908
    .line 2909
    return-void

    .line 2910
    nop

    :pswitch_data_b5e
    .packed-switch 0x1
        :pswitch_997
        :pswitch_969
        :pswitch_969
        :pswitch_95c
        :pswitch_957
        :pswitch_954
        :pswitch_939
        :pswitch_954
        :pswitch_934
        :pswitch_92f
        :pswitch_92a
        :pswitch_909
        :pswitch_909
        :pswitch_95c
        :pswitch_904
        :pswitch_8fb
        :pswitch_8d4
        :pswitch_8d4
        :pswitch_89b
        :pswitch_89b
        :pswitch_858
        :pswitch_828
        :pswitch_7e8
        :pswitch_7e8
        :pswitch_7e2
        :pswitch_7e2
        :pswitch_7e2
    .end packed-switch
.end method
