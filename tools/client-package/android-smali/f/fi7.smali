.class public abstract Lf/fi7;
.super Ljava/lang/Object;


# static fields
.field public static final um1:Lf/xv7;


# direct methods
.method static constructor <clinit>()V
    .registers 1

    .line 1
    const-class v0, Lf/fi7;

    .line 2
    .line 3
    invoke-static {v0}, Lf/tv7;->I80(Ljava/lang/Class;)Lf/xv7;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    sput-object v0, Lf/fi7;->um1:Lf/xv7;

    .line 8
    .line 9
    return-void
.end method

.method public static Mo(Ljava/nio/ByteBuffer;)V
    # MonMMO-EX: three more locals (v22-v24); v22 keeps section 6's flag word past the form loop.
    .registers 26

    .line 1
    move-object/from16 v0, p0

    .line 2
    .line 3
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 4
    .line 5
    .line 6
    move-result v1

    .line 7
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 8
    .line 9
    .line 10
    move-result v2

    .line 11
    const/4 v3, 0x3

    .line 12
    const/4 v4, 0x2

    .line 13
    const/4 v5, 0x0

    .line 14
    const/4 v6, 0x1

    .line 15
    packed-switch v1, :pswitch_data_ab8

    .line 16
    .line 17
    .line 18
    :pswitch_11
    invoke-virtual {v0}, Ljava/nio/Buffer;->position()I

    .line 19
    .line 20
    .line 21
    move-result v1

    .line 22
    add-int/2addr v1, v2

    .line 23
    invoke-virtual {v0, v1}, Ljava/nio/ByteBuffer;->position(I)Ljava/nio/Buffer;

    .line 24
    .line 25
    .line 26
    return-void

    .line 27
    :pswitch_1a
    sget v1, Lf/pi4;->AuX:I

    .line 28
    .line 29
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 30
    .line 31
    .line 32
    move-result v1

    .line 33
    :goto_20
    if-ge v5, v1, :cond_ab6

    .line 34
    .line 35
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 36
    .line 37
    .line 38
    move-result v2

    .line 39
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 40
    .line 41
    .line 42
    move-result v3

    .line 43
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 44
    .line 45
    .line 46
    move-result v4

    .line 47
    sget-object v6, Lf/e80;->IU0:Lf/e80;

    .line 48
    .line 49
    invoke-static {v2, v3}, Lf/qy4;->ge1(BS)I

    .line 50
    .line 51
    .line 52
    move-result v2

    .line 53
    iget-object v3, v6, Lf/e80;->t40:Lf/j1;

    .line 54
    .line 55
    invoke-virtual {v3, v2, v4}, Lf/j1;->JT(II)V

    .line 56
    .line 57
    .line 58
    add-int/lit8 v5, v5, 0x1

    .line 59
    .line 60
    goto :goto_20

    .line 61
    :pswitch_3c
    sget v1, Lf/is8;->zT0:I

    .line 62
    .line 63
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 64
    .line 65
    .line 66
    move-result v1

    .line 67
    :goto_42
    if-ge v5, v1, :cond_63

    .line 68
    .line 69
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 70
    .line 71
    .line 72
    move-result v2

    .line 73
    invoke-static {v2}, Lf/lb8;->JW1(B)Lf/lb8;

    .line 74
    .line 75
    .line 76
    move-result-object v2

    .line 77
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 78
    .line 79
    .line 80
    move-result v3

    .line 81
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 82
    .line 83
    .line 84
    move-result v4

    .line 85
    sget-object v6, Lf/ae;->bl:Lf/ae;

    .line 86
    .line 87
    new-instance v7, Lf/zs3;

    .line 88
    .line 89
    invoke-direct {v7, v2, v3, v4}, Lf/zs3;-><init>(Lf/lb8;II)V

    .line 90
    .line 91
    .line 92
    iget-object v3, v6, Lf/ae;->WV:Ljava/util/HashMap;

    .line 93
    .line 94
    invoke-virtual {v3, v2, v7}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 95
    .line 96
    .line 97
    add-int/lit8 v5, v5, 0x1

    .line 98
    .line 99
    goto :goto_42

    .line 100
    :cond_63
    sget-object v0, Lf/ae;->bl:Lf/ae;

    .line 101
    .line 102
    iget-object v1, v0, Lf/ae;->WV:Ljava/util/HashMap;

    .line 103
    .line 104
    invoke-virtual {v1}, Ljava/util/HashMap;->isEmpty()Z

    .line 105
    .line 106
    .line 107
    move-result v2

    .line 108
    if-nez v2, :cond_ab6

    .line 109
    .line 110
    invoke-virtual {v1}, Ljava/util/HashMap;->values()Ljava/util/Collection;

    .line 111
    .line 112
    .line 113
    move-result-object v1

    .line 114
    invoke-interface {v1}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    .line 115
    .line 116
    .line 117
    move-result-object v1

    .line 118
    :cond_75
    :goto_75
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    .line 119
    .line 120
    .line 121
    move-result v2

    .line 122
    if-eqz v2, :cond_ab6

    .line 123
    .line 124
    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 125
    .line 126
    .line 127
    move-result-object v2

    .line 128
    check-cast v2, Lf/zs3;

    .line 129
    .line 130
    invoke-virtual {v2}, Lf/zs3;->Aa0()Z

    .line 131
    .line 132
    .line 133
    move-result v3

    .line 134
    if-nez v3, :cond_88

    .line 135
    .line 136
    goto :goto_75

    .line 137
    :cond_88
    iget-object v2, v2, Lf/zs3;->BZ1:Lf/lb8;

    .line 138
    .line 139
    sget-object v3, Lf/lb8;->Uj1:Lf/lb8;

    .line 140
    .line 141
    if-eq v2, v3, :cond_96

    .line 142
    .line 143
    sget-object v3, Lf/lb8;->mB:Lf/lb8;

    .line 144
    .line 145
    if-eq v2, v3, :cond_96

    .line 146
    .line 147
    sget-object v3, Lf/lb8;->rX0:Lf/lb8;

    .line 148
    .line 149
    if-ne v2, v3, :cond_75

    .line 150
    .line 151
    :cond_96
    iput-object v2, v0, Lf/ae;->GA1:Lf/lb8;

    .line 152
    .line 153
    return-void

    .line 154
    :pswitch_99
    sget v1, Lf/zb9;->Lj1:I

    .line 155
    .line 156
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 157
    .line 158
    .line 159
    move-result v1

    .line 160
    const/4 v2, 0x0

    .line 161
    :goto_a0
    if-ge v2, v1, :cond_ab6

    .line 162
    .line 163
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 164
    .line 165
    .line 166
    move-result v3

    .line 167
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 168
    .line 169
    .line 170
    move-result v4

    .line 171
    sget-object v6, Lf/f2;->rt1:Lf/f2;

    .line 172
    .line 173
    iget-object v6, v6, Lf/f2;->Xf:Lf/k33;

    .line 174
    .line 175
    invoke-virtual {v6, v3}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 176
    .line 177
    .line 178
    move-result-object v3

    .line 179
    check-cast v3, Lf/l74;

    .line 180
    .line 181
    iget-object v6, v3, Lf/l74;->by1:Lf/k89;

    .line 182
    .line 183
    invoke-virtual {v6, v4}, Lf/k89;->get(I)Ljava/lang/Object;

    .line 184
    .line 185
    .line 186
    move-result-object v6

    .line 187
    check-cast v6, Lf/cc6;

    .line 188
    .line 189
    if-nez v6, :cond_c6

    .line 190
    .line 191
    new-instance v6, Lf/x12;

    .line 192
    .line 193
    invoke-direct {v6}, Lf/x12;-><init>()V

    .line 194
    .line 195
    .line 196
    invoke-virtual {v3, v4, v6, v5}, Lf/l74;->Mr(ILf/cc6;Z)V

    .line 197
    .line 198
    .line 199
    :cond_c6
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 200
    .line 201
    .line 202
    move-result v3

    .line 203
    and-int/lit8 v4, v3, 0x1

    .line 204
    .line 205
    if-eqz v4, :cond_d4

    .line 206
    .line 207
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 208
    .line 209
    .line 210
    move-result v4

    .line 211
    iput-byte v4, v6, Lf/cc6;->DQ0:B

    .line 212
    .line 213
    :cond_d4
    and-int/lit8 v4, v3, 0x2

    .line 214
    .line 215
    if-eqz v4, :cond_de

    .line 216
    .line 217
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 218
    .line 219
    .line 220
    move-result v4

    .line 221
    iput-byte v4, v6, Lf/cc6;->IM0:B

    .line 222
    .line 223
    :cond_de
    and-int/lit8 v3, v3, 0x4

    .line 224
    .line 225
    if-eqz v3, :cond_e8

    .line 226
    .line 227
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 228
    .line 229
    .line 230
    move-result v3

    .line 231
    iput-byte v3, v6, Lf/cc6;->AA:B

    .line 232
    .line 233
    :cond_e8
    add-int/lit8 v2, v2, 0x1

    .line 234
    .line 235
    goto :goto_a0

    .line 236
    :pswitch_eb
    sget v1, Lf/fb8;->Uo:I

    .line 237
    .line 238
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 239
    .line 240
    .line 241
    move-result v1

    .line 242
    :goto_f1
    if-ge v5, v1, :cond_ab6

    .line 243
    .line 244
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 245
    .line 246
    .line 247
    move-result v2

    .line 248
    sget-object v3, Lf/cz2;->Iv1:Lf/cz2;

    .line 249
    .line 250
    iget-object v3, v3, Lf/cz2;->D50:Lf/k33;

    .line 251
    .line 252
    invoke-virtual {v3, v2}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 253
    .line 254
    .line 255
    move-result-object v4

    .line 256
    check-cast v4, Lf/cc2;

    .line 257
    .line 258
    if-nez v4, :cond_10d

    .line 259
    .line 260
    new-instance v4, Lf/cc2;

    .line 261
    .line 262
    invoke-direct {v4}, Ljava/lang/Object;-><init>()V

    .line 263
    .line 264
    .line 265
    iput-byte v2, v4, Lf/cc2;->YU1:B

    .line 266
    .line 267
    invoke-virtual {v3, v2, v4}, Lf/k33;->xp1(BLjava/lang/Object;)Ljava/lang/Object;

    .line 268
    .line 269
    .line 270
    :cond_10d
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 271
    .line 272
    .line 273
    move-result v2

    .line 274
    and-int/lit8 v3, v2, 0x1

    .line 275
    .line 276
    if-eqz v3, :cond_11b

    .line 277
    .line 278
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 279
    .line 280
    .line 281
    move-result v3

    .line 282
    iput-byte v3, v4, Lf/cc2;->zG1:B

    .line 283
    .line 284
    :cond_11b
    and-int/lit8 v3, v2, 0x2

    .line 285
    .line 286
    if-eqz v3, :cond_125

    .line 287
    .line 288
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 289
    .line 290
    .line 291
    move-result v3

    .line 292
    iput-byte v3, v4, Lf/cc2;->J80:B

    .line 293
    .line 294
    :cond_125
    and-int/lit8 v2, v2, 0x4

    .line 295
    .line 296
    if-eqz v2, :cond_12c

    .line 297
    .line 298
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 299
    .line 300
    .line 301
    :cond_12c
    add-int/lit8 v5, v5, 0x1

    .line 302
    .line 303
    goto :goto_f1

    .line 304
    :pswitch_12f
    sget v1, Lf/nd0;->L9:I

    .line 305
    .line 306
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 307
    .line 308
    .line 309
    move-result v1

    .line 310
    const/4 v2, 0x0

    .line 311
    :goto_136
    if-ge v2, v1, :cond_ab6

    .line 312
    .line 313
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 314
    .line 315
    .line 316
    move-result v3

    .line 317
    sget-object v4, Lf/cz2;->Iv1:Lf/cz2;

    .line 318
    .line 319
    iget-object v4, v4, Lf/cz2;->xD:Lf/ch4;

    .line 320
    .line 321
    invoke-virtual {v4, v3}, Lf/ch4;->xH0(S)Ljava/lang/Object;

    .line 322
    .line 323
    .line 324
    move-result-object v6

    .line 325
    check-cast v6, Lf/br7;

    .line 326
    .line 327
    if-nez v6, :cond_15d

    .line 328
    .line 329
    new-instance v6, Lf/br7;

    .line 330
    .line 331
    sget-object v7, Lf/te1;->mo:Lf/te1;

    .line 332
    .line 333
    invoke-direct {v6}, Ljava/lang/Object;-><init>()V

    .line 334
    .line 335
    .line 336
    iput-byte v5, v6, Lf/br7;->GI0:B

    .line 337
    .line 338
    iput-object v7, v6, Lf/br7;->Rr:Lf/te1;

    .line 339
    .line 340
    sget-object v7, Lf/br7;->ui0:[B

    .line 341
    .line 342
    iput-object v7, v6, Lf/br7;->zM:[B

    .line 343
    .line 344
    invoke-static {v7}, Ljava/util/Arrays;->sort([B)V

    .line 345
    .line 346
    .line 347
    invoke-virtual {v4, v3, v6}, Lf/ch4;->SB0(SLjava/lang/Object;)Ljava/lang/Object;

    .line 348
    .line 349
    .line 350
    :cond_15d
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 351
    .line 352
    .line 353
    move-result v3

    .line 354
    and-int/lit8 v4, v3, 0x1

    .line 355
    .line 356
    if-eqz v4, :cond_16b

    .line 357
    .line 358
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 359
    .line 360
    .line 361
    move-result v4

    .line 362
    iput-byte v4, v6, Lf/br7;->GI0:B

    .line 363
    .line 364
    :cond_16b
    and-int/lit8 v4, v3, 0x2

    .line 365
    .line 366
    if-eqz v4, :cond_182

    .line 367
    .line 368
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 369
    .line 370
    .line 371
    move-result v4

    .line 372
    sget-object v7, Lf/te1;->Lv1:Lf/k33;

    .line 373
    .line 374
    invoke-virtual {v7, v4}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 375
    .line 376
    .line 377
    move-result-object v7

    .line 378
    check-cast v7, Lf/te1;

    .line 379
    .line 380
    const-class v8, Lf/te1;

    .line 381
    .line 382
    invoke-static {v7, v8, v4}, Lf/qy4;->y30(Ljava/lang/Object;Ljava/lang/Class;B)V

    .line 383
    .line 384
    .line 385
    iput-object v7, v6, Lf/br7;->Rr:Lf/te1;

    .line 386
    .line 387
    :cond_182
    and-int/lit8 v4, v3, 0x4

    .line 388
    .line 389
    if-eqz v4, :cond_189

    .line 390
    .line 391
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 392
    .line 393
    .line 394
    :cond_189
    and-int/lit8 v3, v3, 0x8

    .line 395
    .line 396
    if-eqz v3, :cond_1a1

    .line 397
    .line 398
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 399
    .line 400
    .line 401
    move-result v3

    .line 402
    new-array v4, v3, [B

    .line 403
    .line 404
    const/4 v7, 0x0

    .line 405
    :goto_194
    if-ge v7, v3, :cond_19f

    .line 406
    .line 407
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 408
    .line 409
    .line 410
    move-result v8

    .line 411
    aput-byte v8, v4, v7

    .line 412
    .line 413
    add-int/lit8 v7, v7, 0x1

    .line 414
    .line 415
    goto :goto_194

    .line 416
    :cond_19f
    iput-object v4, v6, Lf/br7;->zM:[B

    .line 417
    .line 418
    :cond_1a1
    add-int/lit8 v2, v2, 0x1

    .line 419
    .line 420
    goto :goto_136

    .line 421
    :pswitch_1a4
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 422
    .line 423
    .line 424
    move-result v1

    .line 425
    const/4 v2, 0x0

    .line 426
    :goto_1a9
    if-ge v2, v1, :cond_ab6

    .line 427
    .line 428
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 429
    .line 430
    .line 431
    move-result v3

    .line 432
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 433
    .line 434
    .line 435
    move-result v4

    .line 436
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 437
    .line 438
    .line 439
    move-result v6

    .line 440
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 441
    .line 442
    .line 443
    move-result v7

    .line 444
    if-lez v7, :cond_1e6

    .line 445
    .line 446
    new-instance v8, Ljava/util/ArrayList;

    .line 447
    .line 448
    invoke-direct {v8}, Ljava/util/ArrayList;-><init>()V

    .line 449
    .line 450
    .line 451
    const/4 v9, 0x0

    .line 452
    :goto_1c3
    if-ge v9, v7, :cond_1dd

    .line 453
    .line 454
    new-instance v10, Lf/gd1;

    .line 455
    .line 456
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 457
    .line 458
    .line 459
    move-result v11

    .line 460
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 461
    .line 462
    .line 463
    move-result v12

    .line 464
    invoke-direct {v10}, Ljava/lang/Object;-><init>()V

    .line 465
    .line 466
    .line 467
    iput-short v11, v10, Lf/gd1;->eg0:S

    .line 468
    .line 469
    iput-short v12, v10, Lf/gd1;->vY:S

    .line 470
    .line 471
    invoke-virtual {v8, v10}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 472
    .line 473
    .line 474
    add-int/lit8 v9, v9, 0x1

    .line 475
    .line 476
    int-to-short v9, v9

    .line 477
    goto :goto_1c3

    .line 478
    :cond_1dd
    new-array v7, v5, [Lf/gd1;

    .line 479
    .line 480
    invoke-virtual {v8, v7}, Ljava/util/ArrayList;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;

    .line 481
    .line 482
    .line 483
    move-result-object v7

    .line 484
    check-cast v7, [Lf/gd1;

    .line 485
    .line 486
    goto :goto_1e8

    .line 487
    :cond_1e6
    sget-object v7, Lf/eo5;->Ae1:[Lf/gd1;

    .line 488
    .line 489
    :goto_1e8
    sget-object v8, Lf/er5;->Ul0:Lf/er5;

    .line 490
    .line 491
    new-instance v9, Lf/eo5;

    .line 492
    .line 493
    invoke-direct {v9}, Ljava/lang/Object;-><init>()V

    .line 494
    .line 495
    .line 496
    iput-object v7, v9, Lf/eo5;->Yi0:[Lf/gd1;

    .line 497
    .line 498
    iput-short v4, v9, Lf/eo5;->iK1:S

    .line 499
    .line 500
    iput-short v6, v9, Lf/eo5;->qt:S

    .line 501
    .line 502
    iget-object v4, v8, Lf/er5;->k41:Lf/ch4;

    .line 503
    .line 504
    invoke-virtual {v4, v3, v9}, Lf/ch4;->SB0(SLjava/lang/Object;)Ljava/lang/Object;

    .line 505
    .line 506
    .line 507
    add-int/lit8 v2, v2, 0x1

    .line 508
    .line 509
    int-to-short v2, v2

    .line 510
    goto :goto_1a9

    .line 511
    :pswitch_1fe
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 512
    .line 513
    .line 514
    move-result v1

    .line 515
    const/4 v2, 0x0

    .line 516
    :goto_203
    if-ge v2, v1, :cond_ab6

    .line 517
    .line 518
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 519
    .line 520
    .line 521
    move-result v3

    .line 522
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 523
    .line 524
    .line 525
    move-result v7

    .line 526
    const/4 v8, 0x1

    .line 527
    :goto_20e
    add-int/lit8 v9, v7, 0x1

    .line 528
    .line 529
    if-ge v8, v9, :cond_23e

    .line 530
    .line 531
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 532
    .line 533
    .line 534
    move-result v9

    .line 535
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 536
    .line 537
    .line 538
    move-result-object v10

    .line 539
    invoke-virtual {v10, v9}, Lf/y91;->wT0(S)Lf/zp3;

    .line 540
    .line 541
    .line 542
    move-result-object v9

    .line 543
    if-eqz v9, :cond_23a

    .line 544
    .line 545
    iget-object v10, v9, Lf/zp3;->jY1:[S

    .line 546
    .line 547
    if-ltz v3, :cond_236

    .line 548
    .line 549
    array-length v11, v10

    .line 550
    if-lt v3, v11, :cond_228

    .line 551
    .line 552
    goto :goto_236

    .line 553
    :cond_228
    if-ne v3, v4, :cond_232

    .line 554
    .line 555
    iget-short v9, v9, Lf/zp3;->Kj1:S

    .line 556
    .line 557
    const/16 v11, 0x1ee

    .line 558
    .line 559
    if-ne v9, v11, :cond_232

    .line 560
    .line 561
    const/4 v9, 0x0

    .line 562
    goto :goto_233

    .line 563
    :cond_232
    move v9, v8

    .line 564
    :goto_233
    aput-short v9, v10, v3

    .line 565
    .line 566
    goto :goto_23a

    .line 567
    :cond_236
    :goto_236
    array-length v9, v10

    .line 568
    sub-int/2addr v9, v6

    .line 569
    aput-short v8, v10, v9

    .line 570
    .line 571
    :cond_23a
    :goto_23a
    add-int/lit8 v8, v8, 0x1

    .line 572
    .line 573
    int-to-short v8, v8

    .line 574
    goto :goto_20e

    .line 575
    :cond_23e
    add-int/lit8 v2, v2, 0x1

    .line 576
    .line 577
    int-to-byte v2, v2

    .line 578
    goto :goto_203

    .line 579
    :pswitch_242
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 580
    .line 581
    .line 582
    move-result v1

    .line 583
    const/4 v2, 0x0

    .line 584
    :goto_247
    if-ge v2, v1, :cond_ab6

    .line 585
    .line 586
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 587
    .line 588
    .line 589
    move-result v7

    .line 590
    new-instance v8, Lf/zp3;

    .line 591
    .line 592
    invoke-direct {v8, v7}, Lf/zp3;-><init>(S)V

    .line 593
    .line 594
    .line 595
    # Retail: section 10 creates every species hidden from the Pokedex - that is how the client keeps
    # its reserved 1000-1052 block out of the lists. Species data adds are listed by section 6's
    # MonMMO-EX bit 0x4000 instead.
    iput-boolean v6, v8, Lf/zp3;->Nm1:Z

    .line 596
    .line 597
    sget-object v7, Lf/lo1;->aR:Lf/lo1;

    .line 598
    .line 599
    iput-object v7, v8, Lf/zp3;->Gj:Lf/lo1;

    .line 600
    .line 601
    iput-object v7, v8, Lf/zp3;->wa:Lf/lo1;

    .line 602
    .line 603
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 604
    .line 605
    .line 606
    move-result v7

    .line 607
    invoke-static {v7}, Lf/eb6;->Bd(B)Lf/eb6;

    .line 608
    .line 609
    .line 610
    move-result-object v7

    .line 611
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 612
    .line 613
    .line 614
    move-result v9

    .line 615
    invoke-static {v9}, Lf/eb6;->Bd(B)Lf/eb6;

    .line 616
    .line 617
    .line 618
    move-result-object v9

    .line 619
    iput-object v7, v8, Lf/zp3;->native:Lf/eb6;

    .line 620
    .line 621
    iput-object v9, v8, Lf/zp3;->yE1:Lf/eb6;

    .line 622
    .line 623
    sget-object v7, Lf/r59;->Vu0:[Lf/r59;

    .line 624
    .line 625
    array-length v9, v7

    .line 626
    const/4 v10, 0x0

    .line 627
    :goto_272
    if-ge v10, v9, :cond_280

    .line 628
    .line 629
    aget-object v11, v7, v10

    .line 630
    .line 631
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 632
    .line 633
    .line 634
    move-result v12

    .line 635
    invoke-virtual {v8, v11, v12}, Lf/zp3;->Yw1(Lf/r59;I)V

    .line 636
    .line 637
    .line 638
    add-int/lit8 v10, v10, 0x1

    .line 639
    .line 640
    goto :goto_272

    .line 641
    :cond_280
    const/4 v7, 0x0

    .line 642
    :goto_281
    if-ge v7, v3, :cond_293

    .line 643
    .line 644
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 645
    .line 646
    .line 647
    move-result v9

    .line 648
    if-ltz v7, :cond_290

    .line 649
    .line 650
    if-le v7, v4, :cond_28c

    .line 651
    .line 652
    goto :goto_290

    .line 653
    :cond_28c
    iget-object v10, v8, Lf/zp3;->Kc0:[S

    .line 654
    .line 655
    aput-short v9, v10, v7

    .line 656
    .line 657
    :cond_290
    :goto_290
    add-int/lit8 v7, v7, 0x1

    .line 658
    .line 659
    goto :goto_281

    .line 660
    :cond_293
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 661
    .line 662
    .line 663
    move-result v7

    .line 664
    iput-short v7, v8, Lf/zp3;->VF0:S

    .line 665
    .line 666
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 667
    .line 668
    .line 669
    move-result-object v7

    .line 670
    iget-object v7, v7, Lf/y91;->Uv:Ljava/util/HashMap;

    .line 671
    .line 672
    iget-short v9, v8, Lf/zp3;->Kj1:S

    .line 673
    .line 674
    invoke-static {v9}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 675
    .line 676
    .line 677
    move-result-object v9

    .line 678
    invoke-virtual {v7, v9, v8}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 679
    .line 680
    .line 681
    add-int/lit8 v2, v2, 0x1

    .line 682
    .line 683
    goto :goto_247

    .line 684
    :pswitch_2ab
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 685
    .line 686
    .line 687
    move-result v1

    .line 688
    :goto_2af
    if-ge v5, v1, :cond_ab6

    .line 689
    .line 690
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 691
    .line 692
    .line 693
    move-result v2

    .line 694
    sget-object v3, Lf/ld;->aW0:Lf/ld;

    .line 695
    .line 696
    new-instance v4, Lf/od8;

    .line 697
    .line 698
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 699
    .line 700
    .line 701
    move-result v6

    .line 702
    sget-object v7, Lf/mc;->dA0:Lf/k33;

    .line 703
    .line 704
    invoke-virtual {v7, v6}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 705
    .line 706
    .line 707
    move-result-object v6

    .line 708
    check-cast v6, Lf/mc;

    .line 709
    .line 710
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 711
    .line 712
    .line 713
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 714
    .line 715
    .line 716
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getFloat()F

    .line 717
    .line 718
    .line 719
    move-result v7

    .line 720
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 721
    .line 722
    .line 723
    invoke-direct {v4}, Ljava/lang/Object;-><init>()V

    .line 724
    .line 725
    .line 726
    iput-short v2, v4, Lf/od8;->CK0:S

    .line 727
    .line 728
    iput-object v6, v4, Lf/od8;->k20:Lf/mc;

    .line 729
    .line 730
    iput v7, v4, Lf/od8;->ny:F

    .line 731
    .line 732
    iget-object v3, v3, Lf/ld;->Rz:Lf/ch4;

    .line 733
    .line 734
    invoke-virtual {v3, v2, v4}, Lf/ch4;->SB0(SLjava/lang/Object;)Ljava/lang/Object;

    .line 735
    .line 736
    .line 737
    add-int/lit8 v5, v5, 0x1

    .line 738
    .line 739
    goto :goto_2af

    .line 740
    :pswitch_2e3
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 741
    .line 742
    .line 743
    move-result v1

    .line 744
    const/4 v2, 0x0

    .line 745
    :goto_2e8
    if-ge v2, v1, :cond_ab6

    .line 746
    .line 747
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 748
    .line 749
    .line 750
    move-result v3

    .line 751
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 752
    .line 753
    .line 754
    move-result v4

    .line 755
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 756
    .line 757
    .line 758
    move-result v6

    .line 759
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 760
    .line 761
    .line 762
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 763
    .line 764
    .line 765
    move-result v7

    .line 766
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 767
    .line 768
    .line 769
    move-result v8

    .line 770
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 771
    .line 772
    .line 773
    move-result v9

    .line 774
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 775
    .line 776
    .line 777
    move-result v10

    .line 778
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 779
    .line 780
    .line 781
    move-result v11

    .line 782
    sget-object v12, Lf/vj8;->Bc0:Lf/vj8;

    .line 783
    .line 784
    new-instance v13, Lf/xc7;

    .line 785
    .line 786
    sget-object v14, Lf/lb8;->P50:Lf/k33;

    .line 787
    .line 788
    invoke-virtual {v14, v10}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 789
    .line 790
    .line 791
    move-result-object v14

    .line 792
    check-cast v14, Lf/lb8;

    .line 793
    .line 794
    const-class v15, Lf/lb8;

    .line 795
    .line 796
    invoke-static {v14, v15, v10}, Lf/qy4;->y30(Ljava/lang/Object;Ljava/lang/Class;B)V

    .line 797
    .line 798
    .line 799
    sget-object v10, Lf/da5;->sK0:Lf/k33;

    .line 800
    .line 801
    invoke-virtual {v10, v11}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 802
    .line 803
    .line 804
    move-result-object v10

    .line 805
    check-cast v10, Lf/da5;

    .line 806
    .line 807
    const-class v15, Lf/da5;

    .line 808
    .line 809
    invoke-static {v10, v15, v11}, Lf/qy4;->y30(Ljava/lang/Object;Ljava/lang/Class;B)V

    .line 810
    .line 811
    .line 812
    invoke-direct {v13}, Ljava/lang/Object;-><init>()V

    .line 813
    .line 814
    .line 815
    iput v5, v13, Lf/xc7;->Dl0:I

    .line 816
    .line 817
    iput-short v5, v13, Lf/xc7;->k9:S

    .line 818
    .line 819
    iput-byte v3, v13, Lf/xc7;->I31:B

    .line 820
    .line 821
    iput-byte v4, v13, Lf/xc7;->Ok0:B

    .line 822
    .line 823
    iput v6, v13, Lf/xc7;->P30:I

    .line 824
    .line 825
    iput-byte v7, v13, Lf/xc7;->xe1:B

    .line 826
    .line 827
    iput-byte v8, v13, Lf/xc7;->Dc0:B

    .line 828
    .line 829
    iput-short v9, v13, Lf/xc7;->Uf0:S

    .line 830
    .line 831
    iput-object v14, v13, Lf/xc7;->fP0:Lf/lb8;

    .line 832
    .line 833
    iget-object v4, v12, Lf/vj8;->se:Lf/k33;

    .line 834
    .line 835
    invoke-virtual {v4, v3, v13}, Lf/k33;->xp1(BLjava/lang/Object;)Ljava/lang/Object;

    .line 836
    .line 837
    .line 838
    add-int/lit8 v2, v2, 0x1

    .line 839
    .line 840
    goto :goto_2e8

    .line 841
    :pswitch_348
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 842
    .line 843
    .line 844
    move-result v1

    .line 845
    const/4 v2, 0x0

    .line 846
    :goto_34d
    if-ge v2, v1, :cond_ab6

    .line 847
    .line 848
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 849
    .line 850
    .line 851
    move-result v3

    .line 852
    const/4 v4, 0x5

    .line 853
    new-array v6, v4, [I

    .line 854
    .line 855
    const/4 v7, 0x0

    .line 856
    :goto_357
    if-ge v7, v4, :cond_362

    .line 857
    .line 858
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 859
    .line 860
    .line 861
    move-result v8

    .line 862
    aput v8, v6, v7

    .line 863
    .line 864
    add-int/lit8 v7, v7, 0x1

    .line 865
    .line 866
    goto :goto_357

    .line 867
    :cond_362
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 868
    .line 869
    .line 870
    move-result v4

    .line 871
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 872
    .line 873
    .line 874
    move-result v7

    .line 875
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 876
    .line 877
    .line 878
    move-result v8

    .line 879
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 880
    .line 881
    .line 882
    move-result v9

    .line 883
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 884
    .line 885
    .line 886
    move-result v10

    .line 887
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 888
    .line 889
    .line 890
    move-result v11

    .line 891
    sget-object v12, Lf/an8;->LU:Lf/an8;

    .line 892
    .line 893
    invoke-virtual {v12}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 894
    .line 895
    .line 896
    new-instance v13, Ljava/util/ArrayList;

    .line 897
    .line 898
    invoke-direct {v13}, Ljava/util/ArrayList;-><init>()V

    .line 899
    .line 900
    .line 901
    iget-object v12, v12, Lf/an8;->lO:Ljava/util/TreeMap;

    .line 902
    .line 903
    invoke-virtual {v12}, Ljava/util/TreeMap;->values()Ljava/util/Collection;

    .line 904
    .line 905
    .line 906
    move-result-object v12

    .line 907
    invoke-interface {v12}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    .line 908
    .line 909
    .line 910
    move-result-object v12

    .line 911
    :cond_38e
    :goto_38e
    invoke-interface {v12}, Ljava/util/Iterator;->hasNext()Z

    .line 912
    .line 913
    .line 914
    move-result v14

    .line 915
    if-eqz v14, :cond_3a8

    .line 916
    .line 917
    invoke-interface {v12}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 918
    .line 919
    .line 920
    move-result-object v14

    .line 921
    check-cast v14, Lf/ls0;

    .line 922
    .line 923
    iget-short v15, v14, Lf/ls0;->C4:S

    .line 924
    .line 925
    if-eq v15, v3, :cond_3a4

    .line 926
    .line 927
    iget-short v15, v14, Lf/ls0;->Lpt3:S

    .line 928
    .line 929
    if-lez v15, :cond_38e

    .line 930
    .line 931
    if-ne v15, v3, :cond_38e

    .line 932
    .line 933
    :cond_3a4
    invoke-virtual {v13, v14}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 934
    .line 935
    .line 936
    goto :goto_38e

    .line 937
    :cond_3a8
    invoke-virtual {v13}, Ljava/util/ArrayList;->size()I

    .line 938
    .line 939
    .line 940
    move-result v3

    .line 941
    const/4 v12, 0x0

    .line 942
    :goto_3ad
    if-ge v12, v3, :cond_3c6

    .line 943
    .line 944
    invoke-virtual {v13, v12}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 945
    .line 946
    .line 947
    move-result-object v14

    .line 948
    add-int/lit8 v12, v12, 0x1

    .line 949
    .line 950
    check-cast v14, Lf/ls0;

    .line 951
    .line 952
    iput-object v6, v14, Lf/ls0;->ak0:[I

    .line 953
    .line 954
    iput-byte v4, v14, Lf/ls0;->n01:B

    .line 955
    .line 956
    iput-byte v7, v14, Lf/ls0;->n:B

    .line 957
    .line 958
    iput-short v8, v14, Lf/ls0;->dv:S

    .line 959
    .line 960
    iput-byte v9, v14, Lf/ls0;->tb1:B

    .line 961
    .line 962
    iput-byte v10, v14, Lf/ls0;->YW0:B

    .line 963
    .line 964
    iput-short v11, v14, Lf/ls0;->aD:S

    .line 965
    .line 966
    goto :goto_3ad

    .line 967
    :cond_3c6
    add-int/lit8 v2, v2, 0x1

    .line 968
    .line 969
    goto :goto_34d

    .line 970
    :pswitch_3c9
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 971
    .line 972
    .line 973
    move-result v1

    .line 974
    const/4 v2, 0x0

    .line 975
    :goto_3ce
    if-ge v2, v1, :cond_ab6

    .line 976
    .line 977
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 978
    .line 979
    .line 980
    move-result v7

    .line 981
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 982
    .line 983
    .line 984
    move-result v8

    # MonMMO-EX: the form loop below reuses v8 as its counter; the bits past 0x400 still need it.
    move/from16 v22, v8

    .line 985
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 986
    .line 987
    .line 988
    move-result-object v9

    .line 989
    invoke-virtual {v9, v7}, Lf/y91;->wT0(S)Lf/zp3;

    .line 990
    .line 991
    .line 992
    move-result-object v9

    .line 993
    if-nez v9, :cond_3e7

    .line 994
    .line 995
    new-instance v9, Lf/zp3;

    .line 996
    .line 997
    invoke-direct {v9, v7}, Lf/zp3;-><init>(S)V

    .line 998
    .line 999
    .line 1000
    :cond_3e7
    and-int/lit8 v7, v8, 0x1

    .line 1001
    .line 1002
    if-eqz v7, :cond_405

    .line 1003
    .line 1004
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1005
    .line 1006
    .line 1007
    move-result v7

    .line 1008
    sget-object v10, Lf/lo1;->AY:Lf/k33;

    .line 1009
    .line 1010
    invoke-virtual {v10, v7}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 1011
    .line 1012
    .line 1013
    move-result-object v7

    .line 1014
    check-cast v7, Lf/lo1;

    .line 1015
    .line 1016
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1017
    .line 1018
    .line 1019
    move-result v11

    .line 1020
    invoke-virtual {v10, v11}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 1021
    .line 1022
    .line 1023
    move-result-object v10

    .line 1024
    check-cast v10, Lf/lo1;

    .line 1025
    .line 1026
    iput-object v7, v9, Lf/zp3;->Gj:Lf/lo1;

    .line 1027
    .line 1028
    iput-object v10, v9, Lf/zp3;->wa:Lf/lo1;

    .line 1029
    .line 1030
    :cond_405
    and-int/lit8 v7, v8, 0x2

    .line 1031
    .line 1032
    if-eqz v7, :cond_40b

    .line 1033
    .line 1034
    iput-boolean v6, v9, Lf/zp3;->Nm1:Z

    .line 1035
    .line 1036
    :cond_40b
    and-int/lit8 v7, v8, 0x40

    .line 1037
    .line 1038
    if-eqz v7, :cond_411

    .line 1039
    .line 1040
    iput-boolean v6, v9, Lf/zp3;->bI1:Z

    .line 1041
    .line 1042
    :cond_411
    and-int/lit8 v7, v8, 0x4

    .line 1043
    .line 1044
    if-eqz v7, :cond_42f

    .line 1045
    .line 1046
    sget-object v7, Lf/r59;->TR:[Lf/r59;

    .line 1047
    .line 1048
    array-length v10, v7

    .line 1049
    const/4 v11, 0x0

    .line 1050
    :goto_419
    if-ge v11, v10, :cond_42f

    .line 1051
    .line 1052
    aget-object v12, v7, v11

    .line 1053
    .line 1054
    iget-boolean v13, v12, Lf/r59;->Ny:Z

    .line 1055
    .line 1056
    if-eqz v13, :cond_422

    .line 1057
    .line 1058
    goto :goto_42c

    .line 1059
    :cond_422
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1060
    .line 1061
    .line 1062
    move-result v13

    .line 1063
    const/4 v14, -0x1

    .line 1064
    if-eq v13, v14, :cond_42c

    .line 1065
    .line 1066
    invoke-virtual {v9, v12, v13}, Lf/zp3;->Yw1(Lf/r59;I)V

    .line 1067
    .line 1068
    .line 1069
    :cond_42c
    :goto_42c
    add-int/lit8 v11, v11, 0x1

    .line 1070
    .line 1071
    goto :goto_419

    .line 1072
    :cond_42f
    and-int/lit8 v7, v8, 0x8

    .line 1073
    .line 1074
    if-eqz v7, :cond_446

    .line 1075
    .line 1076
    const/4 v7, 0x0

    .line 1077
    :goto_434
    if-ge v7, v3, :cond_446

    .line 1078
    .line 1079
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1080
    .line 1081
    .line 1082
    move-result v10

    .line 1083
    if-ltz v7, :cond_443

    .line 1084
    .line 1085
    if-le v7, v4, :cond_43f

    .line 1086
    .line 1087
    goto :goto_443

    .line 1088
    :cond_43f
    iget-object v11, v9, Lf/zp3;->Kc0:[S

    .line 1089
    .line 1090
    aput-short v10, v11, v7

    .line 1091
    .line 1092
    :cond_443
    :goto_443
    add-int/lit8 v7, v7, 0x1

    .line 1093
    .line 1094
    goto :goto_434

    .line 1095
    :cond_446
    and-int/lit8 v7, v8, 0x10

    .line 1096
    .line 1097
    if-eqz v7, :cond_453

    .line 1098
    .line 1099
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1100
    .line 1101
    .line 1102
    move-result v7

    .line 1103
    iput-short v7, v9, Lf/zp3;->mt:S

    .line 1104
    .line 1105
    invoke-virtual {v9}, Lf/zp3;->LU()V

    .line 1106
    .line 1107
    .line 1108
    :cond_453
    and-int/lit8 v7, v8, 0x20

    .line 1109
    .line 1110
    if-eqz v7, :cond_46b

    .line 1111
    .line 1112
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1113
    .line 1114
    .line 1115
    move-result v7

    .line 1116
    new-array v10, v7, [S

    .line 1117
    .line 1118
    const/4 v11, 0x0

    .line 1119
    :goto_45e
    if-ge v11, v7, :cond_469

    .line 1120
    .line 1121
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1122
    .line 1123
    .line 1124
    move-result v12

    .line 1125
    aput-short v12, v10, v11

    .line 1126
    .line 1127
    add-int/lit8 v11, v11, 0x1

    .line 1128
    .line 1129
    goto :goto_45e

    .line 1130
    :cond_469
    iput-object v10, v9, Lf/zp3;->Tf1:[S

    .line 1131
    .line 1132
    :cond_46b
    and-int/lit16 v7, v8, 0x80

    .line 1133
    .line 1134
    if-eqz v7, :cond_475

    .line 1135
    .line 1136
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1137
    .line 1138
    .line 1139
    move-result v7

    .line 1140
    iput v7, v9, Lf/zp3;->bj:I

    .line 1141
    .line 1142
    :cond_475
    and-int/lit16 v7, v8, 0x100

    .line 1143
    .line 1144
    if-eqz v7, :cond_48c

    .line 1145
    .line 1146
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1147
    .line 1148
    .line 1149
    move-result v7

    .line 1150
    sget-object v10, Lf/wq3;->MP1:Lf/k33;

    .line 1151
    .line 1152
    invoke-virtual {v10, v7}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 1153
    .line 1154
    .line 1155
    move-result-object v10

    .line 1156
    check-cast v10, Lf/wq3;

    .line 1157
    .line 1158
    const-class v11, Lf/wq3;

    .line 1159
    .line 1160
    invoke-static {v10, v11, v7}, Lf/qy4;->y30(Ljava/lang/Object;Ljava/lang/Class;B)V

    .line 1161
    .line 1162
    .line 1163
    iput-object v10, v9, Lf/zp3;->wd1:Lf/wq3;

    .line 1164
    .line 1165
    :cond_48c
    and-int/lit16 v7, v8, 0x200

    .line 1166
    .line 1167
    if-eqz v7, :cond_492

    .line 1168
    .line 1169
    iput-boolean v6, v9, Lf/zp3;->Vt1:Z

    .line 1170
    .line 1171
    :cond_492
    and-int/lit16 v7, v8, 0x400

    .line 1172
    .line 1173
    if-eqz v7, :cond_4df

    .line 1174
    .line 1175
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1176
    .line 1177
    .line 1178
    move-result v7

    .line 1179
    const/4 v8, 0x0

    .line 1180
    :goto_49b
    if-ge v8, v7, :cond_4df

    .line 1181
    .line 1182
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1183
    .line 1184
    .line 1185
    move-result v10

    .line 1186
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1187
    .line 1188
    .line 1189
    move-result v12

    .line 1190
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1191
    .line 1192
    .line 1193
    move-result v13

    .line 1194
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1195
    .line 1196
    .line 1197
    move-result v14

    .line 1198
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 1199
    .line 1200
    .line 1201
    move-result v15

    .line 1202
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 1203
    .line 1204
    .line 1205
    move-result v16

    .line 1206
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1207
    .line 1208
    .line 1209
    move-result v11

    .line 1210
    if-eqz v11, :cond_4be

    .line 1211
    .line 1212
    const/16 v17, 0x1

    .line 1213
    .line 1214
    goto :goto_4c0

    .line 1215
    :cond_4be
    const/16 v17, 0x0

    .line 1216
    .line 1217
    :goto_4c0
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1218
    .line 1219
    .line 1220
    move-result v11

    .line 1221
    if-eqz v11, :cond_4c9

    .line 1222
    .line 1223
    const/16 v18, 0x1

    .line 1224
    .line 1225
    goto :goto_4cb

    .line 1226
    :cond_4c9
    const/16 v18, 0x0

    .line 1227
    .line 1228
    :goto_4cb
    iget-object v11, v9, Lf/zp3;->Pq:Lf/k33;

    .line 1229
    .line 1230
    move-object/from16 v19, v11

    .line 1231
    .line 1232
    new-instance v11, Lf/p13;

    .line 1233
    .line 1234
    move-object/from16 v3, v19

    .line 1235
    .line 1236
    invoke-direct/range {v11 .. v18}, Lf/p13;-><init>(SSSIIZZ)V

    .line 1237
    .line 1238
    .line 1239
    iput-boolean v6, v11, Lf/p13;->Fa1:Z

    .line 1240
    .line 1241
    invoke-virtual {v3, v10, v11}, Lf/k33;->xp1(BLjava/lang/Object;)Ljava/lang/Object;

    .line 1242
    .line 1243
    .line 1244
    add-int/lit8 v8, v8, 0x1

    .line 1245
    .line 1246
    const/4 v3, 0x3

    .line 1247
    goto :goto_49b

    .line 1248
    :cond_4df
    # MonMMO-EX: species fields only the ROM loader (v67.SB) used to fill, so a species data adds
    # carries everything a ROM species does. Payloads follow 0x400 in bit order:
    #   0x800  u8 growth (o9 key, the ROM byte), u16 base exp, u16 height dm, u16 weight hg
    #   0x1000 u8 count, then per evolution u8 method (vj3 ROM key), u16 param, u16 target,
    #          u8 time (param is final: item methods already carry the ROM loader's +5000)
    #   0x2000 u8 type1, u8 type2 (client type ids, as section 10 writes them)
    move/from16 v8, v22
    and-int/lit16 v7, v8, 0x800
    if-eqz v7, :mmx_scalars_done
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B
    move-result v7
    sget-object v10, Lf/o9;->Gy1:Lf/k33;
    invoke-virtual {v10, v7}, Lf/k33;->VK0(B)Ljava/lang/Object;
    move-result-object v10
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S
    move-result v11
    const v7, 0xffff
    and-int/2addr v11, v7
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S
    move-result v12
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S
    move-result v13
    iput v11, v9, Lf/zp3;->pu1:I
    iput-short v12, v9, Lf/zp3;->YV1:S
    iput-short v13, v9, Lf/zp3;->Rj:S
    if-eqz v10, :mmx_scalars_done
    check-cast v10, Lf/o9;
    iput-object v10, v9, Lf/zp3;->R:Lf/o9;

    :mmx_scalars_done
    and-int/lit16 v7, v8, 0x1000
    if-eqz v7, :mmx_evolutions_done
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B
    move-result v10
    and-int/lit16 v10, v10, 0xff
    const/4 v11, 0x0

    :mmx_evolution_next
    if-ge v11, v10, :mmx_evolutions_done
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B
    move-result v12
    sget-object v13, Lf/vj3;->Zo0:Lf/k33;
    invoke-virtual {v13, v12}, Lf/k33;->VK0(B)Ljava/lang/Object;
    move-result-object v12
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S
    move-result v13
    const v14, 0xffff
    and-int/2addr v13, v14
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S
    move-result v14
    # The time of day a method key cannot carry (0 any, 1 day, 2 night) rides above the u16
    # parameter; the evolution tab (f/j67) splits it off again and draws the sun or moon badge.
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B
    move-result v7
    and-int/lit16 v7, v7, 0xff
    shl-int/lit8 v7, v7, 0x10
    or-int/2addr v13, v7
    add-int/lit8 v11, v11, 0x1
    if-eqz v12, :mmx_evolution_next
    check-cast v12, Lf/vj3;
    new-instance v15, Lf/b81;
    invoke-direct {v15}, Ljava/lang/Object;-><init>()V
    iput-object v12, v15, Lf/b81;->uR0:Lf/vj3;
    iput v13, v15, Lf/b81;->zw0:I
    iput-short v14, v15, Lf/b81;->P6:S
    iget-object v12, v9, Lf/zp3;->tg:Ljava/util/ArrayList;
    invoke-virtual {v12, v15}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
    # The ROM loader's two link passes, for this one entry: the target's U5 is its direct
    # pre-evolution and its pRn the root of the chain (walked up through U5).
    invoke-static {}, Lf/y91;->xk0()Lf/y91;
    move-result-object v12
    invoke-virtual {v12, v14}, Lf/y91;->wT0(S)Lf/zp3;
    move-result-object v12
    if-eqz v12, :mmx_evolution_next
    iput-object v9, v12, Lf/zp3;->U5:Lf/zp3;
    move-object v13, v9

    :mmx_evolution_root
    iget-object v15, v13, Lf/zp3;->U5:Lf/zp3;
    if-eqz v15, :mmx_evolution_rooted
    if-eq v15, v12, :mmx_evolution_rooted
    move-object v13, v15
    goto :mmx_evolution_root

    :mmx_evolution_rooted
    iput-object v13, v12, Lf/zp3;->pRn:Lf/zp3;
    goto :mmx_evolution_next

    :mmx_evolutions_done
    and-int/lit16 v7, v8, 0x2000
    if-eqz v7, :mmx_types_done
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B
    move-result v7
    invoke-static {v7}, Lf/eb6;->Bd(B)Lf/eb6;
    move-result-object v7
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B
    move-result v10
    invoke-static {v10}, Lf/eb6;->Bd(B)Lf/eb6;
    move-result-object v10
    iput-object v7, v9, Lf/zp3;->native:Lf/eb6;
    iput-object v10, v9, Lf/zp3;->yE1:Lf/eb6;

    :mmx_types_done
    # MonMMO-EX 0x4000: u8 dex listing, for a species data adds (section 10 created it hidden, as
    # retail does). 1 lists it; 2 lists it but outside the National list - a regional form, which
    # its own region tab lists (the constructor put the record id in the National slot, index 5).
    # An alternate form (Mega, Gigantamax, gender, cosmetic) carries no listing and stays hidden:
    # it is reached through its base species. v7/v10 are the handlers' scratch, and v3 is reassigned
    # right after.
    and-int/lit16 v7, v8, 0x4000
    if-eqz v7, :mmx_listing_done
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B
    move-result v7
    const/4 v3, 0x0
    iput-boolean v3, v9, Lf/zp3;->Nm1:Z
    const/4 v10, 0x2
    if-ne v7, v10, :mmx_listing_done
    iget-object v7, v9, Lf/zp3;->jY1:[S
    const/4 v10, 0x5
    aput-short v3, v7, v10
    :mmx_listing_done
    add-int/lit8 v2, v2, 0x1

    .line 1249
    .line 1250
    const/4 v3, 0x3

    .line 1251
    goto/16 :goto_3ce

    .line 1252
    .line 1253
    :pswitch_4e4
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1254
    .line 1255
    .line 1256
    move-result v1

    .line 1257
    const/4 v2, 0x0

    .line 1258
    :goto_4e9
    if-ge v2, v1, :cond_ab6

    .line 1259
    .line 1260
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1261
    .line 1262
    .line 1263
    move-result v7

    .line 1264
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1265
    .line 1266
    .line 1267
    move-result v8

    .line 1268
    new-instance v3, Ljava/util/ArrayList;

    .line 1269
    .line 1270
    invoke-direct {v3}, Ljava/util/ArrayList;-><init>()V

    .line 1271
    .line 1272
    .line 1273
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1274
    .line 1275
    .line 1276
    move-result v4

    .line 1277
    const/4 v6, 0x0

    .line 1278
    :goto_4fd
    if-ge v6, v4, :cond_559

    .line 1279
    .line 1280
    move v9, v6

    .line 1281
    new-instance v6, Lf/nl2;

    .line 1282
    .line 1283
    move v10, v9

    .line 1284
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1285
    .line 1286
    .line 1287
    move-result v9

    .line 1288
    move v11, v10

    .line 1289
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1290
    .line 1291
    .line 1292
    move-result v10

    .line 1293
    move v12, v11

    .line 1294
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1295
    .line 1296
    .line 1297
    move-result v11

    .line 1298
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1299
    .line 1300
    .line 1301
    move-result v13

    .line 1302
    sget-object v14, Lf/ug4;->xC1:Lf/k33;

    .line 1303
    .line 1304
    invoke-virtual {v14, v13}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 1305
    .line 1306
    .line 1307
    move-result-object v13

    .line 1308
    check-cast v13, Lf/ug4;

    .line 1309
    .line 1310
    move v14, v12

    .line 1311
    move-object v12, v13

    .line 1312
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1313
    .line 1314
    .line 1315
    move-result v13

    .line 1316
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1317
    .line 1318
    .line 1319
    move-result v15

    .line 1320
    invoke-static {v15}, Lf/b38;->eA0(B)Lf/b38;

    .line 1321
    .line 1322
    .line 1323
    move-result-object v15

    .line 1324
    move/from16 v16, v14

    .line 1325
    .line 1326
    move-object v14, v15

    .line 1327
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1328
    .line 1329
    .line 1330
    move-result v15

    .line 1331
    move/from16 v17, v16

    .line 1332
    .line 1333
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1334
    .line 1335
    .line 1336
    move-result v16

    .line 1337
    move/from16 v18, v17

    .line 1338
    .line 1339
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1340
    .line 1341
    .line 1342
    move-result v17

    .line 1343
    move/from16 v19, v18

    .line 1344
    .line 1345
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1346
    .line 1347
    .line 1348
    move-result v18

    .line 1349
    move/from16 v20, v19

    .line 1350
    .line 1351
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1352
    .line 1353
    .line 1354
    move-result v19

    .line 1355
    move/from16 v21, v20

    .line 1356
    .line 1357
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1358
    .line 1359
    .line 1360
    move-result v20

    .line 1361
    invoke-direct/range {v6 .. v20}, Lf/nl2;-><init>(SBBSBLf/ug4;BLf/b38;SSSSBB)V

    .line 1362
    .line 1363
    .line 1364
    invoke-virtual {v3, v6}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 1365
    .line 1366
    .line 1367
    add-int/lit8 v6, v21, 0x1

    .line 1368
    .line 1369
    goto :goto_4fd

    .line 1370
    :cond_559
    sget-object v4, Lf/fi8;->PC:Lf/fi8;

    .line 1371
    .line 1372
    iget-object v6, v4, Lf/fi8;->J20:Lf/k89;

    .line 1373
    .line 1374
    iget-object v7, v4, Lf/fi8;->Wg0:Lf/ch4;

    .line 1375
    .line 1376
    iget-object v8, v4, Lf/fi8;->Bd0:Lf/k89;

    .line 1377
    .line 1378
    invoke-virtual {v3}, Ljava/util/ArrayList;->size()I

    .line 1379
    .line 1380
    .line 1381
    move-result v9

    .line 1382
    const/4 v10, 0x0

    .line 1383
    :goto_566
    if-ge v10, v9, :cond_5c8

    .line 1384
    .line 1385
    invoke-virtual {v3, v10}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 1386
    .line 1387
    .line 1388
    move-result-object v11

    .line 1389
    add-int/lit8 v10, v10, 0x1

    .line 1390
    .line 1391
    check-cast v11, Lf/nl2;

    .line 1392
    .line 1393
    const/16 v12, 0x400

    .line 1394
    .line 1395
    invoke-virtual {v11, v12}, Lf/nl2;->uM0(S)Z

    .line 1396
    .line 1397
    .line 1398
    move-result v12

    .line 1399
    iget-short v13, v11, Lf/nl2;->T70:S

    .line 1400
    .line 1401
    iget-byte v14, v11, Lf/nl2;->iC:B

    .line 1402
    .line 1403
    if-eqz v12, :cond_581

    .line 1404
    .line 1405
    iget-object v12, v4, Lf/fi8;->cb1:Ljava/util/ArrayList;

    .line 1406
    .line 1407
    invoke-virtual {v12, v11}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 1408
    .line 1409
    .line 1410
    :cond_581
    iget-short v12, v11, Lf/nl2;->A10:S

    .line 1411
    .line 1412
    invoke-static {v14, v12}, Lf/qy4;->ge1(BS)I

    .line 1413
    .line 1414
    .line 1415
    move-result v12

    .line 1416
    invoke-virtual {v8, v12}, Lf/k89;->get(I)Ljava/lang/Object;

    .line 1417
    .line 1418
    .line 1419
    move-result-object v15

    .line 1420
    check-cast v15, Ljava/util/List;

    .line 1421
    .line 1422
    if-nez v15, :cond_597

    .line 1423
    .line 1424
    new-instance v15, Ljava/util/ArrayList;

    .line 1425
    .line 1426
    invoke-direct {v15}, Ljava/util/ArrayList;-><init>()V

    .line 1427
    .line 1428
    .line 1429
    invoke-virtual {v8, v12, v15}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    .line 1430
    .line 1431
    .line 1432
    :cond_597
    invoke-interface {v15, v11}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 1433
    .line 1434
    .line 1435
    invoke-virtual {v7, v13}, Lf/ch4;->xH0(S)Ljava/lang/Object;

    .line 1436
    .line 1437
    .line 1438
    move-result-object v12

    .line 1439
    check-cast v12, Ljava/util/List;

    .line 1440
    .line 1441
    if-nez v12, :cond_5aa

    .line 1442
    .line 1443
    new-instance v12, Ljava/util/ArrayList;

    .line 1444
    .line 1445
    invoke-direct {v12}, Ljava/util/ArrayList;-><init>()V

    .line 1446
    .line 1447
    .line 1448
    invoke-virtual {v7, v13, v12}, Lf/ch4;->SB0(SLjava/lang/Object;)Ljava/lang/Object;

    .line 1449
    .line 1450
    .line 1451
    :cond_5aa
    invoke-interface {v12, v11}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 1452
    .line 1453
    .line 1454
    iget-byte v12, v11, Lf/nl2;->rk1:B

    .line 1455
    .line 1456
    int-to-short v12, v12

    .line 1457
    invoke-static {v14, v12}, Lf/qy4;->ge1(BS)I

    .line 1458
    .line 1459
    .line 1460
    move-result v12

    .line 1461
    invoke-virtual {v6, v12}, Lf/k89;->get(I)Ljava/lang/Object;

    .line 1462
    .line 1463
    .line 1464
    move-result-object v13

    .line 1465
    check-cast v13, Ljava/util/List;

    .line 1466
    .line 1467
    if-nez v13, :cond_5c4

    .line 1468
    .line 1469
    new-instance v13, Ljava/util/ArrayList;

    .line 1470
    .line 1471
    invoke-direct {v13}, Ljava/util/ArrayList;-><init>()V

    .line 1472
    .line 1473
    .line 1474
    invoke-virtual {v6, v12, v13}, Lf/k89;->Cp(ILjava/lang/Object;)Ljava/lang/Object;

    .line 1475
    .line 1476
    .line 1477
    :cond_5c4
    invoke-interface {v13, v11}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 1478
    .line 1479
    .line 1480
    goto :goto_566

    .line 1481
    :cond_5c8
    add-int/lit8 v2, v2, 0x1

    .line 1482
    .line 1483
    goto/16 :goto_4e9

    .line 1484
    .line 1485
    :pswitch_5cc
    new-instance v1, Lf/qw6;

    .line 1486
    .line 1487
    invoke-direct {v1}, Lf/sg1;-><init>()V

    .line 1488
    .line 1489
    .line 1490
    iput-object v0, v1, Lf/sg1;->zb:Ljava/nio/ByteBuffer;

    .line 1491
    .line 1492
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1493
    .line 1494
    .line 1495
    move-result v2

    .line 1496
    const/4 v3, 0x0

    .line 1497
    :goto_5d8
    if-ge v3, v2, :cond_ab6

    .line 1498
    .line 1499
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1500
    .line 1501
    .line 1502
    move-result v4

    .line 1503
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1504
    .line 1505
    .line 1506
    move-result v7

    .line 1507
    sget-object v8, Lf/dd9;->Db1:Lf/k33;

    .line 1508
    .line 1509
    invoke-virtual {v8, v7}, Lf/cu2;->FW(B)Z

    .line 1510
    .line 1511
    .line 1512
    move-result v9

    .line 1513
    if-eqz v9, :cond_5f1

    .line 1514
    .line 1515
    invoke-virtual {v8, v7}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 1516
    .line 1517
    .line 1518
    move-result-object v7

    .line 1519
    check-cast v7, Lf/dd9;

    .line 1520
    .line 1521
    goto :goto_5f3

    .line 1522
    :cond_5f1
    sget-object v7, Lf/dd9;->xy0:Lf/dd9;

    .line 1523
    .line 1524
    :goto_5f3
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1525
    .line 1526
    .line 1527
    move-result v8

    .line 1528
    invoke-static {}, Lf/k92;->dh0()Lf/k92;

    .line 1529
    .line 1530
    .line 1531
    move-result-object v9

    .line 1532
    invoke-virtual {v9, v4}, Lf/k92;->BW1(S)Lf/hu6;

    .line 1533
    .line 1534
    .line 1535
    move-result-object v9

    .line 1536
    if-nez v9, :cond_613

    .line 1537
    .line 1538
    new-instance v9, Lf/hu6;

    .line 1539
    .line 1540
    sget-object v10, Lf/eb6;->Nu:Lf/eb6;

    .line 1541
    .line 1542
    invoke-direct {v9, v4, v10}, Lf/hu6;-><init>(SLf/eb6;)V

    .line 1543
    .line 1544
    .line 1545
    invoke-static {}, Lf/k92;->dh0()Lf/k92;

    .line 1546
    .line 1547
    .line 1548
    move-result-object v4

    .line 1549
    iget-object v4, v4, Lf/k92;->EY1:Lf/ch4;

    .line 1550
    .line 1551
    iget-short v10, v9, Lf/hu6;->m21:S

    .line 1552
    .line 1553
    invoke-virtual {v4, v10, v9}, Lf/ch4;->SB0(SLjava/lang/Object;)Ljava/lang/Object;

    .line 1554
    .line 1555
    .line 1556
    :cond_613
    iput-object v7, v9, Lf/hu6;->iB0:Lf/dd9;

    .line 1557
    .line 1558
    and-int/lit8 v4, v8, 0x1

    .line 1559
    .line 1560
    if-eqz v4, :cond_61f

    .line 1561
    .line 1562
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1563
    .line 1564
    .line 1565
    move-result v4

    .line 1566
    iput-byte v4, v9, Lf/hu6;->dv1:B

    .line 1567
    .line 1568
    :cond_61f
    and-int/lit8 v4, v8, 0x2

    .line 1569
    .line 1570
    if-eqz v4, :cond_629

    .line 1571
    .line 1572
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1573
    .line 1574
    .line 1575
    move-result v4

    .line 1576
    iput-short v4, v9, Lf/hu6;->r9:S

    .line 1577
    .line 1578
    :cond_629
    and-int/lit8 v4, v8, 0x4

    .line 1579
    .line 1580
    if-eqz v4, :cond_633

    .line 1581
    .line 1582
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1583
    .line 1584
    .line 1585
    move-result v4

    .line 1586
    iput-byte v4, v9, Lf/hu6;->g41:B

    .line 1587
    .line 1588
    :cond_633
    and-int/lit8 v4, v8, 0x8

    .line 1589
    .line 1590
    if-eqz v4, :cond_641

    .line 1591
    .line 1592
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1593
    .line 1594
    .line 1595
    move-result v4

    .line 1596
    invoke-static {v4}, Lf/eb6;->Bd(B)Lf/eb6;

    .line 1597
    .line 1598
    .line 1599
    move-result-object v4

    .line 1600
    iput-object v4, v9, Lf/hu6;->n61:Lf/eb6;

    .line 1601
    .line 1602
    :cond_641
    and-int/lit8 v4, v8, 0x10

    .line 1603
    .line 1604
    if-eqz v4, :cond_64b

    .line 1605
    .line 1606
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 1607
    .line 1608
    .line 1609
    move-result v4

    .line 1610
    iput v4, v9, Lf/hu6;->VX:I

    .line 1611
    .line 1612
    :cond_64b
    and-int/lit8 v4, v8, 0x20

    .line 1613
    .line 1614
    if-eqz v4, :cond_655

    .line 1615
    .line 1616
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1617
    .line 1618
    .line 1619
    move-result v4

    .line 1620
    iput-byte v4, v9, Lf/hu6;->xx1:B

    .line 1621
    .line 1622
    :cond_655
    and-int/lit8 v4, v8, 0x40

    .line 1623
    .line 1624
    if-eqz v4, :cond_65f

    .line 1625
    .line 1626
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1627
    .line 1628
    .line 1629
    move-result v4

    .line 1630
    iput-byte v4, v9, Lf/hu6;->Bm:B

    .line 1631
    .line 1632
    :cond_65f
    and-int/lit16 v4, v8, 0x80

    .line 1633
    .line 1634
    if-eqz v4, :cond_665

    .line 1635
    .line 1636
    const/4 v4, 0x1

    .line 1637
    goto :goto_666

    .line 1638
    :cond_665
    const/4 v4, 0x0

    .line 1639
    :goto_666
    iput-boolean v4, v9, Lf/hu6;->BM0:Z

    .line 1640
    .line 1641
    and-int/lit16 v4, v8, 0x100

    .line 1642
    .line 1643
    if-eqz v4, :cond_672

    .line 1644
    .line 1645
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1646
    .line 1647
    .line 1648
    move-result v4

    .line 1649
    iput-byte v4, v9, Lf/hu6;->yx:B

    .line 1650
    .line 1651
    :cond_672
    and-int/lit16 v4, v8, 0x200

    .line 1652
    .line 1653
    if-eqz v4, :cond_6c3

    .line 1654
    .line 1655
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1656
    .line 1657
    .line 1658
    move-result v4

    .line 1659
    new-array v7, v4, [Lf/r59;

    .line 1660
    .line 1661
    const/4 v10, 0x0

    .line 1662
    :goto_67d
    if-ge v10, v4, :cond_68c

    .line 1663
    .line 1664
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1665
    .line 1666
    .line 1667
    move-result v11

    .line 1668
    invoke-static {v11}, Lf/r59;->Wn0(B)Lf/r59;

    .line 1669
    .line 1670
    .line 1671
    move-result-object v11

    .line 1672
    aput-object v11, v7, v10

    .line 1673
    .line 1674
    add-int/lit8 v10, v10, 0x1

    .line 1675
    .line 1676
    goto :goto_67d

    .line 1677
    :cond_68c
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1678
    .line 1679
    .line 1680
    move-result v4

    .line 1681
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1682
    .line 1683
    .line 1684
    move-result v10

    .line 1685
    new-array v11, v10, [Lf/r59;

    .line 1686
    .line 1687
    const/4 v12, 0x0

    .line 1688
    :goto_697
    if-ge v12, v10, :cond_6a6

    .line 1689
    .line 1690
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1691
    .line 1692
    .line 1693
    move-result v13

    .line 1694
    invoke-static {v13}, Lf/r59;->Wn0(B)Lf/r59;

    .line 1695
    .line 1696
    .line 1697
    move-result-object v13

    .line 1698
    aput-object v13, v11, v12

    .line 1699
    .line 1700
    add-int/lit8 v12, v12, 0x1

    .line 1701
    .line 1702
    goto :goto_697

    .line 1703
    :cond_6a6
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1704
    .line 1705
    .line 1706
    move-result v10

    .line 1707
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1708
    .line 1709
    .line 1710
    move-result v12

    .line 1711
    if-ne v12, v6, :cond_6b2

    .line 1712
    .line 1713
    const/4 v12, 0x1

    .line 1714
    goto :goto_6b3

    .line 1715
    :cond_6b2
    const/4 v12, 0x0

    .line 1716
    :goto_6b3
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1717
    .line 1718
    .line 1719
    move-result v13

    .line 1720
    iput-object v7, v9, Lf/hu6;->ah:[Lf/r59;

    .line 1721
    .line 1722
    iput-object v11, v9, Lf/hu6;->W60:[Lf/r59;

    .line 1723
    .line 1724
    iput-byte v4, v9, Lf/hu6;->a71:B

    .line 1725
    .line 1726
    iput-byte v10, v9, Lf/hu6;->N30:B

    .line 1727
    .line 1728
    iput-boolean v12, v9, Lf/hu6;->LC0:Z

    .line 1729
    .line 1730
    iput-byte v13, v9, Lf/hu6;->Ve0:B

    .line 1731
    .line 1732
    :cond_6c3
    and-int/lit16 v4, v8, 0x400

    .line 1733
    .line 1734
    if-eqz v4, :cond_6cd

    .line 1735
    .line 1736
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1737
    .line 1738
    .line 1739
    move-result v4

    .line 1740
    iput-byte v4, v9, Lf/hu6;->om1:B

    .line 1741
    .line 1742
    :cond_6cd
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1743
    .line 1744
    .line 1745
    move-result v4

    .line 1746
    new-array v7, v4, [Lf/d06;

    .line 1747
    .line 1748
    const/4 v8, 0x0

    .line 1749
    :goto_6d4
    if-ge v8, v4, :cond_6f6

    .line 1750
    .line 1751
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 1752
    .line 1753
    .line 1754
    move-result v10

    .line 1755
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1756
    .line 1757
    .line 1758
    move-result v11

    .line 1759
    new-array v12, v11, [Lf/kg2;

    .line 1760
    .line 1761
    const/4 v13, 0x0

    .line 1762
    :goto_6e1
    if-ge v13, v11, :cond_6ec

    .line 1763
    .line 1764
    invoke-virtual {v1}, Lf/ih6;->CI0()Lf/kg2;

    .line 1765
    .line 1766
    .line 1767
    move-result-object v14

    .line 1768
    aput-object v14, v12, v13

    .line 1769
    .line 1770
    add-int/lit8 v13, v13, 0x1

    .line 1771
    .line 1772
    goto :goto_6e1

    .line 1773
    :cond_6ec
    new-instance v11, Lf/d06;

    .line 1774
    .line 1775
    invoke-direct {v11, v10, v12}, Lf/d06;-><init>(I[Lf/kg2;)V

    .line 1776
    .line 1777
    .line 1778
    aput-object v11, v7, v8

    .line 1779
    .line 1780
    add-int/lit8 v8, v8, 0x1

    .line 1781
    .line 1782
    goto :goto_6d4

    .line 1783
    :cond_6f6
    iput-object v7, v9, Lf/hu6;->OV0:[Lf/d06;

    .line 1784
    .line 1785
    add-int/lit8 v3, v3, 0x1

    .line 1786
    .line 1787
    goto/16 :goto_5d8

    .line 1788
    .line 1789
    :pswitch_6fc
    new-instance v1, Lf/ui0;

    .line 1790
    .line 1791
    invoke-direct {v1}, Lf/sg1;-><init>()V

    .line 1792
    .line 1793
    .line 1794
    iput-object v0, v1, Lf/sg1;->zb:Ljava/nio/ByteBuffer;

    .line 1795
    .line 1796
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1797
    .line 1798
    .line 1799
    move-result v2

    .line 1800
    new-instance v3, Lf/ry6;

    .line 1801
    .line 1802
    invoke-direct {v3}, Lf/ry6;-><init>()V

    .line 1803
    .line 1804
    .line 1805
    new-instance v4, Lf/g08;

    .line 1806
    .line 1807
    invoke-direct {v4}, Lf/g08;-><init>()V

    .line 1808
    .line 1809
    .line 1810
    const/4 v7, 0x0

    .line 1811
    :goto_712
    if-ge v7, v2, :cond_9bb

    .line 1812
    .line 1813
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1814
    .line 1815
    .line 1816
    move-result v8

    .line 1817
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 1818
    .line 1819
    .line 1820
    move-result v9

    .line 1821
    sget-object v10, Lf/an8;->LU:Lf/an8;

    .line 1822
    .line 1823
    invoke-virtual {v10, v8}, Lf/an8;->R3(S)Lf/ls0;

    .line 1824
    .line 1825
    .line 1826
    move-result-object v11

    .line 1827
    and-int/lit8 v12, v9, 0x1

    .line 1828
    .line 1829
    if-eqz v12, :cond_73c

    .line 1830
    .line 1831
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 1832
    .line 1833
    .line 1834
    move-result v12

    .line 1835
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 1836
    .line 1837
    .line 1838
    move-result v13

    .line 1839
    iget v14, v11, Lf/ls0;->FO:I

    .line 1840
    .line 1841
    if-ne v14, v12, :cond_738

    .line 1842
    .line 1843
    invoke-virtual {v11}, Lf/ls0;->ae()I

    .line 1844
    .line 1845
    .line 1846
    move-result v14

    .line 1847
    if-eq v14, v13, :cond_73c

    .line 1848
    .line 1849
    :cond_738
    iput v12, v11, Lf/ls0;->FO:I

    .line 1850
    .line 1851
    iput v13, v11, Lf/ls0;->MS1:I

    .line 1852
    .line 1853
    :cond_73c
    and-int/lit8 v12, v9, 0x2

    .line 1854
    .line 1855
    if-eqz v12, :cond_756

    .line 1856
    .line 1857
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1858
    .line 1859
    .line 1860
    move-result v12

    .line 1861
    if-ne v12, v6, :cond_748

    .line 1862
    .line 1863
    const/4 v12, 0x1

    .line 1864
    goto :goto_749

    .line 1865
    :cond_748
    const/4 v12, 0x0

    .line 1866
    :goto_749
    iput-boolean v12, v11, Lf/ls0;->q30:Z

    .line 1867
    .line 1868
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1869
    .line 1870
    .line 1871
    move-result v12

    .line 1872
    if-ne v12, v6, :cond_753

    .line 1873
    .line 1874
    const/4 v12, 0x1

    .line 1875
    goto :goto_754

    .line 1876
    :cond_753
    const/4 v12, 0x0

    .line 1877
    :goto_754
    iput-boolean v12, v11, Lf/ls0;->CS1:Z

    .line 1878
    .line 1879
    :cond_756
    and-int/lit8 v12, v9, 0x4

    .line 1880
    .line 1881
    if-eqz v12, :cond_76a

    .line 1882
    .line 1883
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1884
    .line 1885
    .line 1886
    move-result v12

    .line 1887
    invoke-static {v12}, Lf/nq6;->Wp0(I)Lf/nq6;

    .line 1888
    .line 1889
    .line 1890
    move-result-object v12

    .line 1891
    if-eqz v12, :cond_76a

    .line 1892
    .line 1893
    iput-object v12, v11, Lf/ls0;->o3:Lf/nq6;

    .line 1894
    .line 1895
    iget-short v12, v12, Lf/nq6;->Eq0:S

    .line 1896
    .line 1897
    iput-short v12, v11, Lf/ls0;->I51:S

    .line 1898
    .line 1899
    :cond_76a
    and-int/lit8 v12, v9, 0x8

    .line 1900
    .line 1901
    if-eqz v12, :cond_774

    .line 1902
    .line 1903
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1904
    .line 1905
    .line 1906
    move-result v12

    .line 1907
    iput-short v12, v11, Lf/ls0;->I51:S

    .line 1908
    .line 1909
    :cond_774
    and-int/lit8 v12, v9, 0x10

    .line 1910
    .line 1911
    if-eqz v12, :cond_7a4

    .line 1912
    .line 1913
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1914
    .line 1915
    .line 1916
    move-result v12

    .line 1917
    sget-object v13, Lf/qt4;->m9:Lf/k33;

    .line 1918
    .line 1919
    invoke-virtual {v13, v12}, Lf/cu2;->FW(B)Z

    .line 1920
    .line 1921
    .line 1922
    move-result v14

    .line 1923
    if-eqz v14, :cond_78b

    .line 1924
    .line 1925
    invoke-virtual {v13, v12}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 1926
    .line 1927
    .line 1928
    move-result-object v12

    .line 1929
    check-cast v12, Lf/qt4;

    .line 1930
    .line 1931
    goto :goto_78d

    .line 1932
    :cond_78b
    sget-object v12, Lf/qt4;->p41:Lf/qt4;

    .line 1933
    .line 1934
    :goto_78d
    iput-object v12, v11, Lf/ls0;->Z70:Lf/qt4;

    .line 1935
    .line 1936
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 1937
    .line 1938
    .line 1939
    move-result v12

    .line 1940
    invoke-virtual {v13, v12}, Lf/cu2;->FW(B)Z

    .line 1941
    .line 1942
    .line 1943
    move-result v14

    .line 1944
    if-eqz v14, :cond_7a0

    .line 1945
    .line 1946
    invoke-virtual {v13, v12}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 1947
    .line 1948
    .line 1949
    move-result-object v12

    .line 1950
    check-cast v12, Lf/qt4;

    .line 1951
    .line 1952
    goto :goto_7a2

    .line 1953
    :cond_7a0
    sget-object v12, Lf/qt4;->p41:Lf/qt4;

    .line 1954
    .line 1955
    :goto_7a2
    iput-object v12, v11, Lf/ls0;->dD:Lf/qt4;

    .line 1956
    .line 1957
    :cond_7a4
    and-int/lit8 v12, v9, 0x20

    .line 1958
    .line 1959
    if-eqz v12, :cond_7c7

    .line 1960
    .line 1961
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1962
    .line 1963
    .line 1964
    move-result v12

    .line 1965
    iget-object v13, v10, Lf/an8;->bh:Ljava/util/TreeMap;

    .line 1966
    .line 1967
    invoke-static {v8}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 1968
    .line 1969
    .line 1970
    move-result-object v14

    .line 1971
    invoke-virtual {v10, v12}, Lf/an8;->R3(S)Lf/ls0;

    .line 1972
    .line 1973
    .line 1974
    move-result-object v15

    .line 1975
    invoke-virtual {v13, v14, v15}, Ljava/util/TreeMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 1976
    .line 1977
    .line 1978
    iget-object v13, v10, Lf/an8;->lO:Ljava/util/TreeMap;

    .line 1979
    .line 1980
    invoke-static {v8}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 1981
    .line 1982
    .line 1983
    move-result-object v14

    .line 1984
    invoke-virtual {v13, v14}, Ljava/util/TreeMap;->remove(Ljava/lang/Object;)Ljava/lang/Object;

    .line 1985
    .line 1986
    .line 1987
    iget-object v10, v10, Lf/an8;->C11:Lf/ry6;

    .line 1988
    .line 1989
    invoke-virtual {v10, v8, v12}, Lf/ry6;->VB1(SS)S

    .line 1990
    .line 1991
    .line 1992
    :cond_7c7
    and-int/lit8 v10, v9, 0x40

    .line 1993
    .line 1994
    if-eqz v10, :cond_7d5

    .line 1995
    .line 1996
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 1997
    .line 1998
    .line 1999
    move-result v10

    .line 2000
    invoke-virtual {v3, v8, v10}, Lf/ry6;->VB1(SS)S

    .line 2001
    .line 2002
    .line 2003
    invoke-virtual {v4, v9, v8}, Lf/g08;->rF(IS)V

    .line 2004
    .line 2005
    .line 2006
    :cond_7d5
    and-int/lit16 v8, v9, 0x80

    .line 2007
    .line 2008
    if-eqz v8, :cond_7f6

    .line 2009
    .line 2010
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2011
    .line 2012
    .line 2013
    move-result v8

    .line 2014
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2015
    .line 2016
    .line 2017
    move-result v10

    .line 2018
    if-ne v10, v6, :cond_7e5

    .line 2019
    .line 2020
    const/4 v10, 0x1

    .line 2021
    goto :goto_7e6

    .line 2022
    :cond_7e5
    const/4 v10, 0x0

    .line 2023
    :goto_7e6
    iput-short v8, v11, Lf/ls0;->Bm0:S

    .line 2024
    .line 2025
    iput-boolean v10, v11, Lf/ls0;->y3:Z

    .line 2026
    .line 2027
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2028
    .line 2029
    .line 2030
    move-result v8

    .line 2031
    iput-byte v8, v11, Lf/ls0;->Nz:B

    .line 2032
    .line 2033
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2034
    .line 2035
    .line 2036
    move-result v8

    .line 2037
    iput-byte v8, v11, Lf/ls0;->Le1:B

    .line 2038
    .line 2039
    :cond_7f6
    and-int/lit16 v8, v9, 0x100

    .line 2040
    .line 2041
    if-eqz v8, :cond_800

    .line 2042
    .line 2043
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2044
    .line 2045
    .line 2046
    move-result v8

    .line 2047
    iput-short v8, v11, Lf/ls0;->IO:S

    .line 2048
    .line 2049
    :cond_800
    and-int/lit16 v8, v9, 0x200

    .line 2050
    .line 2051
    if-eqz v8, :cond_80a

    .line 2052
    .line 2053
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2054
    .line 2055
    .line 2056
    move-result v8

    .line 2057
    iput-short v8, v11, Lf/ls0;->lj:S

    .line 2058
    .line 2059
    :cond_80a
    and-int/lit16 v8, v9, 0x400

    .line 2060
    .line 2061
    if-eqz v8, :cond_81e

    .line 2062
    .line 2063
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2064
    .line 2065
    .line 2066
    move-result v8

    .line 2067
    invoke-static {v8}, Lf/r59;->Wn0(B)Lf/r59;

    .line 2068
    .line 2069
    .line 2070
    move-result-object v8

    .line 2071
    iput-object v8, v11, Lf/ls0;->zl0:Lf/r59;

    .line 2072
    .line 2073
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2074
    .line 2075
    .line 2076
    move-result v8

    .line 2077
    iput-short v8, v11, Lf/ls0;->y1:S

    .line 2078
    .line 2079
    :cond_81e
    and-int/lit16 v8, v9, 0x800

    .line 2080
    .line 2081
    if-eqz v8, :cond_833

    .line 2082
    .line 2083
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2084
    .line 2085
    .line 2086
    move-result v8

    .line 2087
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2088
    .line 2089
    .line 2090
    move-result v10

    .line 2091
    if-ne v10, v6, :cond_82e

    .line 2092
    .line 2093
    const/4 v10, 0x1

    .line 2094
    goto :goto_82f

    .line 2095
    :cond_82e
    const/4 v10, 0x0

    .line 2096
    :goto_82f
    iput-short v8, v11, Lf/ls0;->Ow:S

    .line 2097
    .line 2098
    iput-boolean v10, v11, Lf/ls0;->aK1:Z

    .line 2099
    .line 2100
    :cond_833
    and-int/lit16 v8, v9, 0x1000

    .line 2101
    .line 2102
    if-eqz v8, :cond_84d

    .line 2103
    .line 2104
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2105
    .line 2106
    .line 2107
    move-result v8

    .line 2108
    invoke-static {v8}, Lf/c89;->IV(B)Lf/c89;

    .line 2109
    .line 2110
    .line 2111
    move-result-object v8

    .line 2112
    sget-object v10, Lf/c89;->m10:Lf/c89;

    .line 2113
    .line 2114
    iget-byte v10, v8, Lf/c89;->N50:B

    .line 2115
    .line 2116
    add-int/lit16 v12, v10, 0x2af8

    .line 2117
    .line 2118
    iput v12, v11, Lf/ls0;->FU:I

    .line 2119
    .line 2120
    add-int/lit16 v10, v10, 0x2c88

    .line 2121
    .line 2122
    iput v10, v11, Lf/ls0;->Dt:I

    .line 2123
    .line 2124
    iput-object v8, v11, Lf/ls0;->EQ:Lf/c89;

    .line 2125
    .line 2126
    :cond_84d
    and-int/lit16 v8, v9, 0x2000

    .line 2127
    .line 2128
    if-eqz v8, :cond_853

    .line 2129
    .line 2130
    iput-boolean v6, v11, Lf/ls0;->KJ:Z

    .line 2131
    .line 2132
    :cond_853
    and-int/lit16 v8, v9, 0x4000

    .line 2133
    .line 2134
    if-eqz v8, :cond_859

    .line 2135
    .line 2136
    iput-boolean v6, v11, Lf/ls0;->pa1:Z

    .line 2137
    .line 2138
    :cond_859
    const v8, 0x8000

    .line 2139
    .line 2140
    .line 2141
    and-int/2addr v8, v9

    .line 2142
    if-eqz v8, :cond_861

    .line 2143
    .line 2144
    iput-boolean v5, v11, Lf/ls0;->Ja0:Z

    .line 2145
    .line 2146
    :cond_861
    const/high16 v8, 0x10000

    .line 2147
    .line 2148
    and-int/2addr v8, v9

    .line 2149
    if-eqz v8, :cond_876

    .line 2150
    .line 2151
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2152
    .line 2153
    .line 2154
    move-result v8

    .line 2155
    invoke-static {v8}, Lf/eb6;->Bd(B)Lf/eb6;

    .line 2156
    .line 2157
    .line 2158
    move-result-object v8

    .line 2159
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2160
    .line 2161
    .line 2162
    move-result v10

    .line 2163
    iput-object v8, v11, Lf/ls0;->UG0:Lf/eb6;

    .line 2164
    .line 2165
    iput-byte v10, v11, Lf/ls0;->Ae0:B

    .line 2166
    .line 2167
    :cond_876
    const/high16 v8, 0x20000

    .line 2168
    .line 2169
    and-int/2addr v8, v9

    .line 2170
    if-eqz v8, :cond_92d

    .line 2171
    .line 2172
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2173
    .line 2174
    .line 2175
    move-result v8

    .line 2176
    sget-object v10, Lf/ds0;->B20:Lf/k33;

    .line 2177
    .line 2178
    invoke-virtual {v10, v8}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 2179
    .line 2180
    .line 2181
    move-result-object v10

    .line 2182
    check-cast v10, Lf/ds0;

    .line 2183
    .line 2184
    const-class v12, Lf/ds0;

    .line 2185
    .line 2186
    invoke-static {v10, v12, v8}, Lf/qy4;->y30(Ljava/lang/Object;Ljava/lang/Class;B)V

    .line 2187
    .line 2188
    .line 2189
    sget-object v8, Lf/a7;->QR1:[I

    .line 2190
    .line 2191
    iget v12, v10, Lf/ds0;->M51:I

    .line 2192
    .line 2193
    aget v8, v8, v12

    .line 2194
    .line 2195
    packed-switch v8, :pswitch_data_ae0

    .line 2196
    .line 2197
    .line 2198
    goto :goto_8fe

    .line 2199
    :pswitch_896
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2200
    .line 2201
    .line 2202
    move-result v8

    .line 2203
    invoke-static {v8}, Lf/lb8;->JW1(B)Lf/lb8;

    .line 2204
    .line 2205
    .line 2206
    move-result-object v8

    .line 2207
    iput-object v8, v11, Lf/ls0;->pI0:Lf/lb8;

    .line 2208
    .line 2209
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2210
    .line 2211
    .line 2212
    move-result v8

    .line 2213
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2214
    .line 2215
    .line 2216
    move-result v12

    .line 2217
    invoke-static {v8, v12}, Lj$/time/YearMonth;->of(II)Lj$/time/YearMonth;

    .line 2218
    .line 2219
    .line 2220
    move-result-object v8

    .line 2221
    iput-object v8, v11, Lf/ls0;->v41:Lj$/time/YearMonth;

    .line 2222
    .line 2223
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2224
    .line 2225
    .line 2226
    move-result v8

    .line 2227
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2228
    .line 2229
    .line 2230
    move-result v12

    .line 2231
    invoke-static {v8, v12}, Lj$/time/YearMonth;->of(II)Lj$/time/YearMonth;

    .line 2232
    .line 2233
    .line 2234
    move-result-object v8

    .line 2235
    iput-object v8, v11, Lf/ls0;->JR:Lj$/time/YearMonth;

    .line 2236
    .line 2237
    goto :goto_8fe

    .line 2238
    :goto_8bd
    :pswitch_8bd
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2239
    .line 2240
    .line 2241
    move-result v8

    .line 2242
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2243
    .line 2244
    .line 2245
    move-result v12

    .line 2246
    invoke-static {v8, v12}, Lj$/time/YearMonth;->of(II)Lj$/time/YearMonth;

    .line 2247
    .line 2248
    .line 2249
    move-result-object v8

    .line 2250
    iput-object v8, v11, Lf/ls0;->v41:Lj$/time/YearMonth;

    .line 2251
    .line 2252
    goto :goto_8fe

    .line 2253
    :pswitch_8cc
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2254
    .line 2255
    .line 2256
    move-result v8

    .line 2257
    invoke-static {v8}, Lf/lb8;->JW1(B)Lf/lb8;

    .line 2258
    .line 2259
    .line 2260
    move-result-object v8

    .line 2261
    iput-object v8, v11, Lf/ls0;->pI0:Lf/lb8;

    .line 2262
    .line 2263
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2264
    .line 2265
    .line 2266
    move-result v8

    .line 2267
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2268
    .line 2269
    .line 2270
    move-result v12

    .line 2271
    invoke-static {v8, v12}, Lj$/time/YearMonth;->of(II)Lj$/time/YearMonth;

    .line 2272
    .line 2273
    .line 2274
    move-result-object v8

    .line 2275
    iput-object v8, v11, Lf/ls0;->v41:Lj$/time/YearMonth;

    .line 2276
    .line 2277
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2278
    .line 2279
    .line 2280
    move-result v8

    .line 2281
    sget-object v12, Lf/wm5;->zh:Lf/k33;

    .line 2282
    .line 2283
    invoke-virtual {v12, v8}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 2284
    .line 2285
    .line 2286
    move-result-object v8

    .line 2287
    check-cast v8, Lf/wm5;

    .line 2288
    .line 2289
    iput-object v8, v11, Lf/ls0;->KE:Lf/wm5;

    .line 2290
    .line 2291
    goto :goto_8fe

    .line 2292
    :pswitch_8f3
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2293
    .line 2294
    .line 2295
    move-result v8

    .line 2296
    invoke-static {v8}, Lf/lb8;->JW1(B)Lf/lb8;

    .line 2297
    .line 2298
    .line 2299
    move-result-object v8

    .line 2300
    iput-object v8, v11, Lf/ls0;->pI0:Lf/lb8;

    .line 2301
    .line 2302
    goto :goto_8bd

    .line 2303
    :goto_8fe
    iput-object v10, v11, Lf/ls0;->dF0:Lf/ds0;

    .line 2304
    .line 2305
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2306
    .line 2307
    .line 2308
    move-result v8

    .line 2309
    new-array v10, v8, [Lf/d06;

    .line 2310
    .line 2311
    const/4 v12, 0x0

    .line 2312
    :goto_907
    if-ge v12, v8, :cond_929

    .line 2313
    .line 2314
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 2315
    .line 2316
    .line 2317
    move-result v13

    .line 2318
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2319
    .line 2320
    .line 2321
    move-result v14

    .line 2322
    new-array v15, v14, [Lf/kg2;

    .line 2323
    .line 2324
    :goto_913
    if-ge v5, v14, :cond_91e

    .line 2325
    .line 2326
    invoke-virtual {v1}, Lf/ih6;->CI0()Lf/kg2;

    .line 2327
    .line 2328
    .line 2329
    move-result-object v17

    .line 2330
    aput-object v17, v15, v5

    .line 2331
    .line 2332
    add-int/lit8 v5, v5, 0x1

    .line 2333
    .line 2334
    goto :goto_913

    .line 2335
    :cond_91e
    new-instance v5, Lf/d06;

    .line 2336
    .line 2337
    invoke-direct {v5, v13, v15}, Lf/d06;-><init>(I[Lf/kg2;)V

    .line 2338
    .line 2339
    .line 2340
    aput-object v5, v10, v12

    .line 2341
    .line 2342
    add-int/lit8 v12, v12, 0x1

    .line 2343
    .line 2344
    const/4 v5, 0x0

    .line 2345
    goto :goto_907

    .line 2346
    :cond_929
    if-lez v8, :cond_92d

    .line 2347
    .line 2348
    iput-object v10, v11, Lf/ls0;->f80:[Lf/d06;

    .line 2349
    .line 2350
    :cond_92d
    const/high16 v5, 0x40000

    .line 2351
    .line 2352
    and-int/2addr v5, v9

    .line 2353
    if-eqz v5, :cond_934

    .line 2354
    .line 2355
    iput-boolean v6, v11, Lf/ls0;->hY0:Z

    .line 2356
    .line 2357
    :cond_934
    const/high16 v5, 0x80000

    .line 2358
    .line 2359
    and-int/2addr v5, v9

    .line 2360
    if-eqz v5, :cond_952

    .line 2361
    .line 2362
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2363
    .line 2364
    .line 2365
    move-result v5

    .line 2366
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2367
    .line 2368
    .line 2369
    move-result v8

    .line 2370
    sget-object v10, Lf/tx1;->P11:Lf/k33;

    .line 2371
    .line 2372
    invoke-virtual {v10, v8}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 2373
    .line 2374
    .line 2375
    move-result-object v10

    .line 2376
    check-cast v10, Lf/tx1;

    .line 2377
    .line 2378
    const-class v12, Lf/tx1;

    .line 2379
    .line 2380
    invoke-static {v10, v12, v8}, Lf/qy4;->y30(Ljava/lang/Object;Ljava/lang/Class;B)V

    .line 2381
    .line 2382
    .line 2383
    iput-short v5, v11, Lf/ls0;->R11:S

    .line 2384
    .line 2385
    iput-object v10, v11, Lf/ls0;->gm0:Lf/tx1;

    .line 2386
    .line 2387
    :cond_952
    const/high16 v5, 0x100000

    .line 2388
    .line 2389
    and-int/2addr v5, v9

    .line 2390
    if-eqz v5, :cond_95d

    .line 2391
    .line 2392
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2393
    .line 2394
    .line 2395
    move-result v5

    .line 2396
    iput-byte v5, v11, Lf/ls0;->r00:B

    .line 2397
    .line 2398
    :cond_95d
    const/high16 v5, 0x200000

    .line 2399
    .line 2400
    and-int/2addr v5, v9

    .line 2401
    if-eqz v5, :cond_97e

    .line 2402
    .line 2403
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2404
    .line 2405
    .line 2406
    move-result v5

    .line 2407
    new-array v8, v5, [Lf/zp3;

    .line 2408
    .line 2409
    const/4 v10, 0x0

    .line 2410
    :goto_969
    if-ge v10, v5, :cond_97c

    .line 2411
    .line 2412
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 2413
    .line 2414
    .line 2415
    move-result-object v12

    .line 2416
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2417
    .line 2418
    .line 2419
    move-result v13

    .line 2420
    invoke-virtual {v12, v13}, Lf/y91;->wT0(S)Lf/zp3;

    .line 2421
    .line 2422
    .line 2423
    move-result-object v12

    .line 2424
    aput-object v12, v8, v10

    .line 2425
    .line 2426
    add-int/lit8 v10, v10, 0x1

    .line 2427
    .line 2428
    goto :goto_969

    .line 2429
    :cond_97c
    iput-object v8, v11, Lf/ls0;->ug:[Lf/zp3;

    .line 2430
    .line 2431
    :cond_97e
    const/high16 v5, 0x400000

    .line 2432
    .line 2433
    and-int/2addr v5, v9

    .line 2434
    if-eqz v5, :cond_989

    .line 2435
    .line 2436
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 2437
    .line 2438
    .line 2439
    move-result v5

    .line 2440
    iput v5, v11, Lf/ls0;->p0:I

    .line 2441
    .line 2442
    :cond_989
    const/high16 v5, 0x800000

    .line 2443
    .line 2444
    and-int/2addr v5, v9

    .line 2445
    if-eqz v5, :cond_991

    .line 2446
    .line 2447
    const/4 v5, 0x0

    .line 2448
    iput-boolean v5, v11, Lf/ls0;->bu0:Z

    .line 2449
    .line 2450
    :cond_991
    const/high16 v5, 0x1000000

    .line 2451
    .line 2452
    and-int/2addr v5, v9

    .line 2453
    if-eqz v5, :cond_9a0

    .line 2454
    .line 2455
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2456
    .line 2457
    .line 2458
    move-result v5

    .line 2459
    invoke-static {v5}, Lf/lb8;->JW1(B)Lf/lb8;

    .line 2460
    .line 2461
    .line 2462
    move-result-object v5

    .line 2463
    iput-object v5, v11, Lf/ls0;->JU0:Lf/lb8;

    .line 2464
    .line 2465
    :cond_9a0
    const/high16 v5, 0x2000000

    .line 2466
    .line 2467
    and-int/2addr v5, v9

    .line 2468
    if-eqz v5, :cond_9ab

    .line 2469
    .line 2470
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2471
    .line 2472
    .line 2473
    move-result v5

    .line 2474
    iput-byte v5, v11, Lf/ls0;->el:B

    .line 2475
    .line 2476
    :cond_9ab
    const/high16 v5, 0x4000000

    .line 2477
    .line 2478
    and-int/2addr v5, v9

    .line 2479
    if-eqz v5, :cond_9b6

    .line 2480
    .line 2481
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2482
    .line 2483
    .line 2484
    move-result v5

    .line 2485
    iput-short v5, v11, Lf/ls0;->GY0:S

    .line 2486
    .line 2487
    :cond_9b6
    # MonMMO-EX: flag 0x8000000 - an item's own text and icon: i32 name string (FU), i32 description
    # string (Dt), u16 icon id (of, sprites/itemicons/<of>.png). Retail's records never set it; the
    # clone pass below carries it onto a 0x40 clone the way it carries price, pocket and sort.
    const/high16 v5, 0x8000000
    and-int/2addr v5, v9
    if-eqz v5, :mmx_item_text_done
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I
    move-result v5
    iput v5, v11, Lf/ls0;->FU:I
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I
    move-result v5
    iput v5, v11, Lf/ls0;->Dt:I
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S
    move-result v5
    iput-short v5, v11, Lf/ls0;->of:S

    :mmx_item_text_done
    add-int/lit8 v7, v7, 0x1

    .line 2488
    .line 2489
    const/4 v5, 0x0

    .line 2490
    goto/16 :goto_712

    .line 2491
    .line 2492
    :cond_9bb
    invoke-virtual {v3}, Lf/ry6;->EG1()[S

    .line 2493
    .line 2494
    .line 2495
    move-result-object v0

    .line 2496
    array-length v1, v0

    .line 2497
    const/4 v5, 0x0

    .line 2498
    :goto_9c1
    if-ge v5, v1, :cond_ab6

    .line 2499
    .line 2500
    aget-short v2, v0, v5

    .line 2501
    .line 2502
    sget-object v6, Lf/an8;->LU:Lf/an8;

    .line 2503
    .line 2504
    invoke-virtual {v6, v2}, Lf/an8;->R3(S)Lf/ls0;

    .line 2505
    .line 2506
    .line 2507
    move-result-object v7

    .line 2508
    invoke-virtual {v3, v2}, Lf/ry6;->Lg(S)S

    .line 2509
    .line 2510
    .line 2511
    move-result v8

    .line 2512
    invoke-virtual {v6, v8}, Lf/an8;->R3(S)Lf/ls0;

    .line 2513
    .line 2514
    .line 2515
    move-result-object v8

    .line 2516
    invoke-virtual {v8, v2}, Lf/ls0;->lPT2(S)Lf/ls0;

    .line 2517
    .line 2518
    .line 2519
    move-result-object v8

    .line 2520
    invoke-virtual {v6, v8}, Lf/an8;->o8(Lf/ls0;)V

    .line 2521
    .line 2522
    .line 2523
    invoke-virtual {v4, v2}, Lf/g08;->Jl(S)I

    .line 2524
    .line 2525
    .line 2526
    move-result v2

    .line 2527
    and-int/lit8 v6, v2, 0x2

    .line 2528
    .line 2529
    if-eqz v6, :cond_9ec

    .line 2530
    .line 2531
    invoke-virtual {v7}, Lf/ls0;->Nu()Z

    .line 2532
    .line 2533
    .line 2534
    move-result v6

    .line 2535
    iput-boolean v6, v8, Lf/ls0;->q30:Z

    .line 2536
    .line 2537
    iget-boolean v6, v7, Lf/ls0;->CS1:Z

    .line 2538
    .line 2539
    iput-boolean v6, v8, Lf/ls0;->CS1:Z

    .line 2540
    .line 2541
    :cond_9ec
    and-int/lit8 v6, v2, 0x1

    .line 2542
    .line 2543
    if-eqz v6, :cond_9fa

    .line 2544
    .line 2545
    iget v6, v7, Lf/ls0;->FO:I

    .line 2546
    .line 2547
    iput v6, v8, Lf/ls0;->FO:I

    .line 2548
    .line 2549
    invoke-virtual {v7}, Lf/ls0;->ae()I

    .line 2550
    .line 2551
    .line 2552
    move-result v6

    .line 2553
    iput v6, v8, Lf/ls0;->MS1:I

    .line 2554
    .line 2555
    :cond_9fa
    and-int/lit8 v6, v2, 0x4

    .line 2556
    .line 2557
    if-eqz v6, :cond_a06

    .line 2558
    .line 2559
    iget-object v6, v7, Lf/ls0;->o3:Lf/nq6;

    .line 2560
    .line 2561
    iput-object v6, v8, Lf/ls0;->o3:Lf/nq6;

    .line 2562
    .line 2563
    iget-short v6, v6, Lf/nq6;->Eq0:S

    .line 2564
    .line 2565
    iput-short v6, v8, Lf/ls0;->I51:S

    .line 2566
    .line 2567
    :cond_a06
    # MonMMO-EX: a clone takes its record's taught move (0x200) and its own name, description and
    # icon (0x8000000) - otherwise a new TM teaches its donor's move and a new item wears the donor's
    # name. Same pattern as the retail copies above; v6 is scratch here.
    and-int/lit16 v6, v2, 0x200
    if-eqz v6, :mmx_clone_move_done
    iget-short v6, v7, Lf/ls0;->lj:S
    iput-short v6, v8, Lf/ls0;->lj:S

    :mmx_clone_move_done
    const/high16 v6, 0x8000000
    and-int/2addr v6, v2
    if-eqz v6, :mmx_clone_text_done
    iget v6, v7, Lf/ls0;->FU:I
    iput v6, v8, Lf/ls0;->FU:I
    iget v6, v7, Lf/ls0;->Dt:I
    iput v6, v8, Lf/ls0;->Dt:I
    iget-short v6, v7, Lf/ls0;->of:S
    iput-short v6, v8, Lf/ls0;->of:S

    :mmx_clone_text_done
    and-int/lit8 v2, v2, 0x8

    .line 2568
    .line 2569
    if-eqz v2, :cond_a10

    .line 2570
    .line 2571
    invoke-virtual {v8}, Lf/ls0;->DM()S

    .line 2572
    .line 2573
    .line 2574
    move-result v2

    .line 2575
    iput-short v2, v8, Lf/ls0;->I51:S

    .line 2576
    .line 2577
    :cond_a10
    add-int/lit8 v5, v5, 0x1

    .line 2578
    .line 2579
    goto :goto_9c1

    .line 2580
    :pswitch_a13
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2581
    .line 2582
    .line 2583
    move-result v1

    .line 2584
    const/4 v5, 0x0

    .line 2585
    :goto_a18
    if-ge v5, v1, :cond_ab6

    .line 2586
    .line 2587
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2588
    .line 2589
    .line 2590
    move-result v2

    .line 2591
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2592
    .line 2593
    .line 2594
    move-result v3

    .line 2595
    sget-object v4, Lf/rb8;->sj1:Lf/k33;

    .line 2596
    .line 2597
    invoke-virtual {v4, v3}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 2598
    .line 2599
    .line 2600
    move-result-object v3

    .line 2601
    check-cast v3, Lf/rb8;

    .line 2602
    .line 2603
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2604
    .line 2605
    .line 2606
    move-result v4

    .line 2607
    const/16 v7, 0xa

    .line 2608
    .line 2609
    new-array v7, v7, [S

    .line 2610
    .line 2611
    move-object v9, v7

    .line 2612
    const/4 v7, 0x0

    .line 2613
    const/4 v8, 0x0

    .line 2614
    :goto_a35
    if-ge v7, v4, :cond_a54

    .line 2615
    .line 2616
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2617
    .line 2618
    .line 2619
    move-result v10

    .line 2620
    add-int/lit8 v11, v8, 0x1

    .line 2621
    .line 2622
    array-length v12, v9

    .line 2623
    if-le v11, v12, :cond_a4e

    .line 2624
    .line 2625
    array-length v12, v9

    .line 2626
    shl-int/2addr v12, v6

    .line 2627
    invoke-static {v12, v11}, Ljava/lang/Math;->max(II)I

    .line 2628
    .line 2629
    .line 2630
    move-result v12

    .line 2631
    new-array v12, v12, [S

    .line 2632
    .line 2633
    array-length v13, v9

    .line 2634
    const/4 v14, 0x0

    .line 2635
    invoke-static {v9, v14, v12, v14, v13}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V

    .line 2636
    .line 2637
    .line 2638
    move-object v9, v12

    .line 2639
    :cond_a4e
    aput-short v10, v9, v8

    .line 2640
    .line 2641
    add-int/lit8 v7, v7, 0x1

    .line 2642
    .line 2643
    move v8, v11

    .line 2644
    goto :goto_a35

    .line 2645
    :cond_a54
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 2646
    .line 2647
    .line 2648
    move-result-object v4

    .line 2649
    invoke-virtual {v4, v2}, Lf/y91;->wT0(S)Lf/zp3;

    .line 2650
    .line 2651
    .line 2652
    move-result-object v2

    .line 2653
    if-eqz v2, :cond_a76

    .line 2654
    .line 2655
    new-array v4, v8, [S

    .line 2656
    .line 2657
    if-nez v8, :cond_a64

    .line 2658
    .line 2659
    const/4 v14, 0x0

    .line 2660
    goto :goto_a6a

    .line 2661
    :cond_a64
    if-lez v8, :cond_a71

    .line 2662
    .line 2663
    const/4 v14, 0x0

    .line 2664
    invoke-static {v9, v14, v4, v14, v8}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V

    .line 2665
    .line 2666
    .line 2667
    :goto_a6a
    iget-object v2, v2, Lf/zp3;->KB:[[S

    .line 2668
    .line 2669
    iget-byte v3, v3, Lf/rb8;->G71:B

    .line 2670
    .line 2671
    aput-object v4, v2, v3

    .line 2672
    .line 2673
    goto :goto_a77

    .line 2674
    :cond_a71
    const/4 v14, 0x0

    .line 2675
    invoke-static {v14}, Lf/i82;->fb0(I)V

    .line 2676
    .line 2677
    .line 2678
    return-void

    .line 2679
    :cond_a76
    const/4 v14, 0x0

    .line 2680
    :goto_a77
    add-int/lit8 v5, v5, 0x1

    .line 2681
    .line 2682
    goto :goto_a18

    .line 2683
    :pswitch_a7a
    const/4 v14, 0x0

    .line 2684
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2685
    .line 2686
    .line 2687
    move-result v1

    .line 2688
    const/4 v5, 0x0

    .line 2689
    :goto_a80
    if-ge v5, v1, :cond_ab6

    .line 2690
    .line 2691
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2692
    .line 2693
    .line 2694
    move-result v2

    .line 2695
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2696
    .line 2697
    .line 2698
    move-result v3

    .line 2699
    and-int/lit16 v3, v3, 0xff

    .line 2700
    .line 2701
    new-instance v4, Ljava/util/ArrayList;

    .line 2702
    .line 2703
    invoke-direct {v4}, Ljava/util/ArrayList;-><init>()V

    .line 2704
    .line 2705
    .line 2706
    const/4 v6, 0x0

    .line 2707
    :goto_a92
    if-ge v6, v3, :cond_aa7

    .line 2708
    .line 2709
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getShort()S

    .line 2710
    .line 2711
    .line 2712
    move-result v7

    .line 2713
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 2714
    .line 2715
    .line 2716
    move-result v8

    .line 2717
    new-instance v9, Lf/hj5;

    .line 2718
    .line 2719
    invoke-direct {v9, v7, v8}, Lf/hj5;-><init>(SB)V

    .line 2720
    .line 2721
    .line 2722
    invoke-virtual {v4, v9}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 2723
    .line 2724
    .line 2725
    add-int/lit8 v6, v6, 0x1

    .line 2726
    .line 2727
    goto :goto_a92

    .line 2728
    :cond_aa7
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 2729
    .line 2730
    .line 2731
    move-result-object v3

    .line 2732
    invoke-virtual {v3, v2}, Lf/y91;->wT0(S)Lf/zp3;

    .line 2733
    .line 2734
    .line 2735
    move-result-object v2

    .line 2736
    if-eqz v2, :cond_ab3

    .line 2737
    .line 2738
    iput-object v4, v2, Lf/zp3;->qy:Ljava/util/ArrayList;

    .line 2739
    .line 2740
    :cond_ab3
    add-int/lit8 v5, v5, 0x1

    .line 2741
    .line 2742
    goto :goto_a80

    .line 2743
    :cond_ab6
    return-void

    .line 2744
    nop

    .line 2745
    :pswitch_data_ab8
    .packed-switch 0x1
        :pswitch_a7a
        :pswitch_a13
        :pswitch_6fc
        :pswitch_5cc
        :pswitch_4e4
        :pswitch_3c9
        :pswitch_348
        :pswitch_2e3
        :pswitch_2ab
        :pswitch_242
        :pswitch_1fe
        :pswitch_1a4
        :pswitch_12f
        :pswitch_eb
        :pswitch_99
        :pswitch_3c
        :pswitch_11
        :pswitch_1a
    .end packed-switch

    .line 2746
    .line 2747
    .line 2748
    .line 2749
    .line 2750
    .line 2751
    .line 2752
    .line 2753
    .line 2754
    .line 2755
    .line 2756
    .line 2757
    .line 2758
    .line 2759
    .line 2760
    .line 2761
    .line 2762
    .line 2763
    .line 2764
    .line 2765
    .line 2766
    .line 2767
    .line 2768
    .line 2769
    .line 2770
    .line 2771
    .line 2772
    .line 2773
    .line 2774
    .line 2775
    .line 2776
    .line 2777
    .line 2778
    .line 2779
    .line 2780
    .line 2781
    .line 2782
    .line 2783
    .line 2784
    .line 2785
    :pswitch_data_ae0
    .packed-switch 0x1
        :pswitch_8f3
        :pswitch_8f3
        :pswitch_8f3
        :pswitch_8f3
        :pswitch_8cc
        :pswitch_8bd
        :pswitch_896
    .end packed-switch
.end method

.method public static St1()Z
    .registers 5

    .line 1
    sget-object v0, Lf/dq7;->vZ1:Lf/u43;

    .line 2
    .line 3
    const-string v1, "data/data.pak"

    .line 4
    .line 5
    invoke-virtual {v0, v1}, Lf/u43;->G4(Ljava/lang/String;)Lf/rz;

    .line 6
    .line 7
    .line 8
    move-result-object v0

    .line 9
    sget-object v1, Lf/fi7;->um1:Lf/xv7;

    .line 10
    .line 11
    const/4 v2, 0x0

    .line 12
    :try_start_b
    invoke-virtual {v0}, Lf/z46;->yD()[B

    .line 13
    .line 14
    .line 15
    move-result-object v0

    .line 16
    invoke-static {v0}, Lf/il7;->pk1([B)[B

    .line 17
    .line 18
    .line 19
    move-result-object v0

    .line 20
    invoke-static {v0}, Ljava/nio/ByteBuffer;->wrap([B)Ljava/nio/ByteBuffer;

    .line 21
    .line 22
    .line 23
    move-result-object v0

    .line 24
    sget-object v3, Ljava/nio/ByteOrder;->LITTLE_ENDIAN:Ljava/nio/ByteOrder;

    .line 25
    .line 26
    invoke-virtual {v0, v3}, Ljava/nio/ByteBuffer;->order(Ljava/nio/ByteOrder;)Ljava/nio/ByteBuffer;

    .line 27
    .line 28
    .line 29
    move-result-object v0

    .line 30
    invoke-virtual {v0}, Ljava/nio/Buffer;->position()I

    .line 31
    .line 32
    .line 33
    move-result v3

    .line 34
    add-int/lit8 v3, v3, 0x8

    .line 35
    .line 36
    invoke-virtual {v0, v3}, Ljava/nio/ByteBuffer;->position(I)Ljava/nio/Buffer;

    .line 37
    .line 38
    .line 39
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->getInt()I

    .line 40
    .line 41
    .line 42
    move-result v3

    .line 43
    const/16 v4, 0x87

    .line 44
    .line 45
    if-eq v4, v3, :cond_3a

    .line 46
    .line 47
    const-string v0, "Mismatched data.pak version. Expected 135 got {}"

    .line 48
    .line 49
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 50
    .line 51
    .line 52
    move-result-object v3

    .line 53
    invoke-interface {v1, v0, v3}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;)V

    .line 54
    .line 55
    .line 56
    return v2

    .line 57
    :catch_38
    move-exception v0

    .line 58
    goto :goto_49

    .line 59
    :cond_3a
    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->get()B

    .line 60
    .line 61
    .line 62
    move-result v3

    .line 63
    const/4 v4, 0x0

    .line 64
    :goto_3f
    if-ge v4, v3, :cond_47

    .line 65
    .line 66
    invoke-static {v0}, Lf/fi7;->Mo(Ljava/nio/ByteBuffer;)V
    :try_end_44
    .catch Ljava/lang/Exception; {:try_start_b .. :try_end_44} :catch_38

    .line 67
    .line 68
    .line 69
    add-int/lit8 v4, v4, 0x1

    .line 70
    .line 71
    goto :goto_3f

    .line 72
    :cond_47
    const/4 v0, 0x1

    .line 73
    return v0

    .line 74
    :goto_49
    const-string v3, "Error loading data package"

    .line 75
    .line 76
    invoke-interface {v1, v3, v0}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Throwable;)V

    .line 77
    .line 78
    .line 79
    return v2
.end method
