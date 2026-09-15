.class public abstract Lf/xf0;
.super Ljava/lang/Object;


# static fields
.field public static final kZ0:Lf/xv7;

.field public static lS0:Z

.field public static final wx:Lf/yw7;


# direct methods
.method static constructor <clinit>()V
    .registers 5

    .line 1
    const-class v0, Lf/xf0;

    .line 2
    .line 3
    invoke-static {v0}, Lf/tv7;->I80(Ljava/lang/Class;)Lf/xv7;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    sput-object v0, Lf/xf0;->kZ0:Lf/xv7;

    .line 8
    .line 9
    sget-object v0, Lf/mh2;->ge1:Lf/mh2;

    .line 10
    .line 11
    iget-object v0, v0, Lf/mh2;->iB:Ljava/util/Locale;

    .line 12
    .line 13
    sget-object v1, Lf/il7;->b80:Lf/nu6;

    .line 14
    .line 15
    new-instance v2, Lf/yw7;

    .line 16
    .line 17
    const/4 v3, 0x1

    .line 18
    invoke-static {v0}, Lf/zt0;->rM0(Ljava/util/Locale;)Lf/zt0;

    .line 19
    .line 20
    .line 21
    move-result-object v0

    .line 22
    invoke-direct {v2, v1, v3, v0}, Lf/sn6;-><init>(Lf/sn6;ILjava/lang/Object;)V

    .line 23
    .line 24
    .line 25
    sget-object v0, Lf/zl6;->PrN:Lf/zl6;

    .line 26
    .line 27
    new-instance v1, Lf/yw7;

    .line 28
    .line 29
    const/4 v3, 0x3

    .line 30
    invoke-direct {v1, v2, v3, v0}, Lf/sn6;-><init>(Lf/sn6;ILjava/lang/Object;)V

    .line 31
    .line 32
    .line 33
    const/4 v0, -0x2

    .line 34
    invoke-static {v0}, Lf/rz8;->Wx0(I)Lf/rz8;

    .line 35
    .line 36
    .line 37
    move-result-object v0

    .line 38
    new-instance v2, Lf/yw7;

    .line 39
    .line 40
    const/16 v3, 0xd

    .line 41
    .line 42
    invoke-direct {v2, v1, v3, v0}, Lf/sn6;-><init>(Lf/sn6;ILjava/lang/Object;)V

    .line 43
    .line 44
    .line 45
    const/4 v0, 0x0

    .line 46
    const/4 v1, 0x2

    .line 47
    invoke-static {v0, v1}, Lf/hc5;->ZB0(II)Lf/ry1;

    .line 48
    .line 49
    .line 50
    move-result-object v1

    .line 51
    new-instance v3, Lf/yw7;

    .line 52
    .line 53
    const/4 v4, 0x4

    .line 54
    invoke-direct {v3, v2, v4, v1}, Lf/sn6;-><init>(Lf/sn6;ILjava/lang/Object;)V

    .line 55
    .line 56
    .line 57
    sput-object v3, Lf/xf0;->wx:Lf/yw7;

    .line 58
    .line 59
    sput-boolean v0, Lf/xf0;->lS0:Z

    .line 60
    .line 61
    return-void
.end method

.method public static AQ0(Lf/n15;)V
    .registers 11

    .line 1
    const/4 v0, 0x0

    .line 2
    const/4 v1, 0x0

    .line 3
    :goto_2
    const/4 v2, 0x5

    .line 4
    if-ge v1, v2, :cond_62

    .line 5
    .line 6
    sget-object v2, Lf/ye4;->be:Lf/ye4;

    .line 7
    .line 8
    iget-object v2, v2, Lf/ye4;->sp:Lf/k33;

    .line 9
    .line 10
    invoke-virtual {v2, v1}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 11
    .line 12
    .line 13
    move-result-object v2

    .line 14
    check-cast v2, Lf/st;

    .line 15
    .line 16
    if-nez v2, :cond_14

    .line 17
    .line 18
    new-array v2, v0, [I

    .line 19
    .line 20
    goto :goto_1a

    .line 21
    :cond_14
    iget-object v2, v2, Lf/st;->LH0:Lf/k89;

    .line 22
    .line 23
    invoke-virtual {v2}, Lf/k89;->pj()[I

    .line 24
    .line 25
    .line 26
    move-result-object v2

    .line 27
    :goto_1a
    array-length v3, v2

    .line 28
    const/4 v4, 0x0

    .line 29
    :goto_1c
    if-ge v4, v3, :cond_5e

    .line 30
    .line 31
    aget v5, v2, v4

    .line 32
    .line 33
    sget-object v6, Lf/ye4;->be:Lf/ye4;

    .line 34
    .line 35
    invoke-virtual {v6, v1, v5}, Lf/ye4;->Ba0(BI)Lf/qj6;

    .line 36
    .line 37
    .line 38
    move-result-object v6

    .line 39
    new-instance v7, Ljava/util/zip/ZipEntry;

    .line 40
    .line 41
    new-instance v8, Ljava/lang/StringBuilder;

    .line 42
    .line 43
    const-string v9, "sprites/trainersprites/"

    .line 44
    .line 45
    invoke-direct {v8, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 46
    .line 47
    .line 48
    invoke-virtual {v8, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 49
    .line 50
    .line 51
    const-string v9, "/"

    .line 52
    .line 53
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 54
    .line 55
    .line 56
    invoke-virtual {v8, v5}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 57
    .line 58
    .line 59
    const-string v5, ".png"

    .line 60
    .line 61
    invoke-virtual {v8, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 62
    .line 63
    .line 64
    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 65
    .line 66
    .line 67
    move-result-object v5

    .line 68
    invoke-direct {v7, v5}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 69
    .line 70
    .line 71
    invoke-virtual {p0, v7}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 72
    .line 73
    .line 74
    invoke-virtual {v6}, Lf/qj6;->RU0()Lcom/badlogic/gdx/graphics/is0;

    .line 75
    .line 76
    .line 77
    move-result-object v5

    .line 78
    new-instance v6, Lf/u39;

    .line 79
    .line 80
    invoke-direct {v6, p0}, Lf/u39;-><init>(Ljava/io/OutputStream;)V

    .line 81
    .line 82
    .line 83
    invoke-static {v6, v5}, Lcom/badlogic/gdx/graphics/x6;->JZ(Lf/z46;Lcom/badlogic/gdx/graphics/is0;)V

    .line 84
    .line 85
    .line 86
    invoke-virtual {v5}, Lcom/badlogic/gdx/graphics/is0;->dispose()V

    .line 87
    .line 88
    .line 89
    invoke-virtual {p0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 90
    .line 91
    .line 92
    add-int/lit8 v4, v4, 0x1

    .line 93
    .line 94
    goto :goto_1c

    .line 95
    :cond_5e
    add-int/lit8 v1, v1, 0x1

    .line 96
    .line 97
    int-to-byte v1, v1

    .line 98
    goto :goto_2

    .line 99
    :cond_62
    return-void
.end method

.method public static At0(Lf/n15;)V
    .registers 24

    .line 1
    move-object/from16 v0, p0

    .line 2
    .line 3
    const-string v1, "name"

    .line 4
    .line 5
    const-string v2, "species_id"

    .line 6
    .line 7
    const-string v3, "total_encounter"

    .line 8
    .line 9
    const-string v4, "encounter"

    .line 10
    .line 11
    sget-object v5, Lf/p37;->se:Lf/qr3;

    .line 12
    .line 13
    if-nez v5, :cond_f

    .line 14
    .line 15
    goto :goto_13

    .line 16
    :cond_f
    iget-object v5, v5, Lf/eb5;->TI0:Lf/v20;

    .line 17
    .line 18
    if-nez v5, :cond_14

    .line 19
    .line 20
    :goto_13
    return-void

    .line 21
    :cond_14
    new-instance v6, Lorg/json/JSONObject;

    .line 22
    .line 23
    invoke-direct {v6}, Lorg/json/JSONObject;-><init>()V

    .line 24
    .line 25
    .line 26
    sget-object v7, Lf/hz1;->Sq1:[Lf/hz1;

    .line 27
    .line 28
    array-length v8, v7

    .line 29
    const/4 v10, 0x0

    .line 30
    :goto_1d
    if-ge v10, v8, :cond_189

    .line 31
    .line 32
    aget-object v11, v7, v10

    .line 33
    .line 34
    invoke-virtual {v5, v11}, Lf/v20;->Re1(Lf/hz1;)Lf/ey1;

    .line 35
    .line 36
    .line 37
    move-result-object v11

    .line 38
    new-instance v12, Lorg/json/JSONObject;

    .line 39
    .line 40
    invoke-direct {v12}, Lorg/json/JSONObject;-><init>()V

    .line 41
    .line 42
    .line 43
    const-string v13, "type"

    .line 44
    .line 45
    iget-object v14, v11, Lf/ey1;->Q02:Lf/hz1;

    .line 46
    .line 47
    invoke-virtual {v14}, Lf/hz1;->toString()Ljava/lang/String;

    .line 48
    .line 49
    .line 50
    move-result-object v14

    .line 51
    invoke-virtual {v12, v13, v14}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 52
    .line 53
    .line 54
    iget v13, v11, Lf/ey1;->BG0:I

    .line 55
    .line 56
    invoke-virtual {v12, v4, v13}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 57
    .line 58
    .line 59
    iget v13, v11, Lf/ey1;->sh0:I

    .line 60
    .line 61
    invoke-virtual {v12, v3, v13}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 62
    .line 63
    .line 64
    new-instance v13, Lorg/json/JSONArray;

    .line 65
    .line 66
    invoke-direct {v13}, Lorg/json/JSONArray;-><init>()V

    .line 67
    .line 68
    .line 69
    invoke-virtual {v11}, Lf/ey1;->M1()Ljava/util/ArrayList;

    .line 70
    .line 71
    .line 72
    move-result-object v14

    .line 73
    invoke-virtual {v14}, Ljava/util/ArrayList;->size()I

    .line 74
    .line 75
    .line 76
    move-result v15

    .line 77
    const/4 v9, 0x0

    .line 78
    :goto_4d
    if-ge v9, v15, :cond_93

    .line 79
    .line 80
    invoke-virtual {v14, v9}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 81
    .line 82
    .line 83
    move-result-object v17

    .line 84
    add-int/lit8 v9, v9, 0x1

    .line 85
    .line 86
    move-object/from16 v18, v5

    .line 87
    .line 88
    move-object/from16 v5, v17

    .line 89
    .line 90
    check-cast v5, Lf/bi3;

    .line 91
    .line 92
    move-object/from16 v17, v7

    .line 93
    .line 94
    new-instance v7, Lorg/json/JSONObject;

    .line 95
    .line 96
    invoke-direct {v7}, Lorg/json/JSONObject;-><init>()V

    .line 97
    .line 98
    .line 99
    move/from16 v19, v8

    .line 100
    .line 101
    iget-short v8, v5, Lf/bi3;->YQ1:S

    .line 102
    .line 103
    invoke-virtual {v7, v2, v8}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 104
    .line 105
    .line 106
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 107
    .line 108
    .line 109
    move-result-object v8

    .line 110
    move/from16 v20, v9

    .line 111
    .line 112
    iget-short v9, v5, Lf/bi3;->YQ1:S

    .line 113
    .line 114
    invoke-virtual {v8, v9}, Lf/y91;->wT0(S)Lf/zp3;

    .line 115
    .line 116
    .line 117
    move-result-object v8

    .line 118
    const/4 v9, 0x0

    .line 119
    invoke-virtual {v8, v9}, Lf/zp3;->xt(Z)Ljava/lang/String;

    .line 120
    .line 121
    .line 122
    move-result-object v8

    .line 123
    invoke-virtual {v7, v1, v8}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 124
    .line 125
    .line 126
    iget v8, v5, Lf/bi3;->Lm:I

    .line 127
    .line 128
    invoke-virtual {v7, v4, v8}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 129
    .line 130
    .line 131
    iget v5, v5, Lf/bi3;->I71:I

    .line 132
    .line 133
    invoke-virtual {v7, v3, v5}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 134
    .line 135
    .line 136
    invoke-virtual {v13, v7}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    .line 137
    .line 138
    .line 139
    move-object/from16 v7, v17

    .line 140
    .line 141
    move-object/from16 v5, v18

    .line 142
    .line 143
    move/from16 v8, v19

    .line 144
    .line 145
    move/from16 v9, v20

    .line 146
    .line 147
    goto :goto_4d

    .line 148
    :cond_93
    move-object/from16 v18, v5

    .line 149
    .line 150
    move-object/from16 v17, v7

    .line 151
    .line 152
    move/from16 v19, v8

    .line 153
    .line 154
    const-string v5, "data"

    .line 155
    .line 156
    invoke-virtual {v12, v5, v13}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 157
    .line 158
    .line 159
    new-instance v5, Lorg/json/JSONArray;

    .line 160
    .line 161
    invoke-direct {v5}, Lorg/json/JSONArray;-><init>()V

    .line 162
    .line 163
    .line 164
    monitor-enter v11

    .line 165
    :try_start_a4
    new-instance v7, Ljava/util/ArrayList;

    .line 166
    .line 167
    iget-object v8, v11, Lf/ey1;->IA1:Ljava/util/LinkedList;

    .line 168
    .line 169
    invoke-direct {v7, v8}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V
    :try_end_ab
    .catchall {:try_start_a4 .. :try_end_ab} :catchall_186

    .line 170
    .line 171
    .line 172
    monitor-exit v11

    .line 173
    invoke-virtual {v7}, Ljava/util/ArrayList;->size()I

    .line 174
    .line 175
    .line 176
    move-result v8

    .line 177
    const/4 v9, 0x0

    .line 178
    :goto_b1
    if-ge v9, v8, :cond_165

    .line 179
    .line 180
    invoke-virtual {v7, v9}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 181
    .line 182
    .line 183
    move-result-object v13

    .line 184
    add-int/lit8 v9, v9, 0x1

    .line 185
    .line 186
    check-cast v13, Lf/lo2;

    .line 187
    .line 188
    iget-object v14, v13, Lf/lo2;->YN1:Lf/nw0;

    .line 189
    .line 190
    iget-short v15, v13, Lf/lo2;->kh:S

    .line 191
    .line 192
    move-object/from16 v20, v3

    .line 193
    .line 194
    sget-object v3, Lf/lo2;->Yi:Lf/nw0;

    .line 195
    .line 196
    invoke-virtual {v14, v3}, Lf/nw0;->equals(Ljava/lang/Object;)Z

    .line 197
    .line 198
    .line 199
    move-result v3

    .line 200
    if-eqz v3, :cond_cc

    .line 201
    .line 202
    move-object/from16 v3, v20

    .line 203
    .line 204
    goto :goto_b1

    .line 205
    :cond_cc
    sget-object v3, Lf/p37;->se:Lf/qr3;

    .line 206
    .line 207
    iget-object v3, v3, Lf/eb5;->TI0:Lf/v20;

    .line 208
    .line 209
    iget-object v14, v13, Lf/lo2;->Y31:Lf/nw0;

    .line 210
    .line 211
    iget-object v3, v3, Lf/v20;->iL:Ljava/lang/Object;

    .line 212
    .line 213
    check-cast v3, Ljava/util/HashMap;

    .line 214
    .line 215
    invoke-virtual {v3, v14}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    .line 216
    .line 217
    .line 218
    move-result-object v3

    .line 219
    check-cast v3, Lf/dl6;

    .line 220
    .line 221
    new-instance v14, Lorg/json/JSONObject;

    .line 222
    .line 223
    invoke-direct {v14}, Lorg/json/JSONObject;-><init>()V

    .line 224
    .line 225
    .line 226
    invoke-virtual {v14, v2, v15}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 227
    .line 228
    .line 229
    move-object/from16 v21, v2

    .line 230
    .line 231
    const-string v2, "form"

    .line 232
    .line 233
    move-object/from16 v22, v4

    .line 234
    .line 235
    iget-byte v4, v13, Lf/lo2;->Yd0:B

    .line 236
    .line 237
    invoke-virtual {v14, v2, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 238
    .line 239
    .line 240
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 241
    .line 242
    .line 243
    move-result-object v2

    .line 244
    invoke-virtual {v2, v15}, Lf/y91;->wT0(S)Lf/zp3;

    .line 245
    .line 246
    .line 247
    move-result-object v2

    .line 248
    const/4 v4, 0x0

    .line 249
    invoke-virtual {v2, v4}, Lf/zp3;->xt(Z)Ljava/lang/String;

    .line 250
    .line 251
    .line 252
    move-result-object v2

    .line 253
    invoke-virtual {v14, v1, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 254
    .line 255
    .line 256
    const-string v2, "secret_shiny"

    .line 257
    .line 258
    iget-boolean v15, v13, Lf/lo2;->b81:Z

    .line 259
    .line 260
    invoke-virtual {v14, v2, v15}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;

    .line 261
    .line 262
    .line 263
    const-string v2, "alpha"

    .line 264
    .line 265
    iget-boolean v15, v13, Lf/lo2;->FV0:Z

    .line 266
    .line 267
    invoke-virtual {v14, v2, v15}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;

    .line 268
    .line 269
    .line 270
    const-string v2, "gender"

    .line 271
    .line 272
    iget-byte v15, v13, Lf/lo2;->VR0:B

    .line 273
    .line 274
    invoke-virtual {v14, v2, v15}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 275
    .line 276
    .line 277
    if-eqz v3, :cond_138

    .line 278
    .line 279
    const-string v2, "nature_string_id"

    .line 280
    .line 281
    iget-object v15, v3, Lf/dl6;->u7:Lf/y17;

    .line 282
    .line 283
    iget-byte v15, v15, Lf/y17;->Or0:B

    .line 284
    .line 285
    const v16, 0x2bf20

    .line 286
    .line 287
    .line 288
    add-int v15, v15, v16

    .line 289
    .line 290
    invoke-virtual {v14, v2, v15}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 291
    .line 292
    .line 293
    const-string v2, "nature_name"

    .line 294
    .line 295
    iget-object v15, v3, Lf/dl6;->u7:Lf/y17;

    .line 296
    .line 297
    invoke-virtual {v15}, Lf/y17;->rB0()Ljava/lang/String;

    .line 298
    .line 299
    .line 300
    move-result-object v15

    .line 301
    invoke-virtual {v14, v2, v15}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 302
    .line 303
    .line 304
    const-string v2, "ivs"

    .line 305
    .line 306
    invoke-static {v3}, Lf/tq;->PA1(Lf/dl6;)Ljava/lang/String;

    .line 307
    .line 308
    .line 309
    move-result-object v3

    .line 310
    invoke-virtual {v14, v2, v3}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 311
    .line 312
    .line 313
    :cond_138
    const-string v2, "global_encounter"

    .line 314
    .line 315
    iget v3, v13, Lf/lo2;->mb:I

    .line 316
    .line 317
    invoke-virtual {v14, v2, v3}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 318
    .line 319
    .line 320
    const-string v2, "species_encounter"

    .line 321
    .line 322
    iget v3, v13, Lf/lo2;->COn:I

    .line 323
    .line 324
    invoke-virtual {v14, v2, v3}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 325
    .line 326
    .line 327
    const-string v2, "date"

    .line 328
    .line 329
    iget-object v3, v13, Lf/lo2;->ul:Lj$/time/Instant;

    .line 330
    .line 331
    move-object v15, v5

    .line 332
    invoke-virtual {v3}, Lj$/time/Instant;->toEpochMilli()J

    .line 333
    .line 334
    .line 335
    move-result-wide v4

    .line 336
    invoke-virtual {v14, v2, v4, v5}, Lorg/json/JSONObject;->put(Ljava/lang/String;J)Lorg/json/JSONObject;

    .line 337
    .line 338
    .line 339
    const-string v2, "caught"

    .line 340
    .line 341
    iget-boolean v3, v13, Lf/lo2;->lV1:Z

    .line 342
    .line 343
    invoke-virtual {v14, v2, v3}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;

    .line 344
    .line 345
    .line 346
    invoke-virtual {v15, v14}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    .line 347
    .line 348
    .line 349
    move-object v5, v15

    .line 350
    move-object/from16 v3, v20

    .line 351
    .line 352
    move-object/from16 v2, v21

    .line 353
    .line 354
    move-object/from16 v4, v22

    .line 355
    .line 356
    goto/16 :goto_b1

    .line 357
    .line 358
    :cond_165
    move-object/from16 v21, v2

    .line 359
    .line 360
    move-object/from16 v20, v3

    .line 361
    .line 362
    move-object/from16 v22, v4

    .line 363
    .line 364
    move-object v15, v5

    .line 365
    const-string v2, "history"

    .line 366
    .line 367
    invoke-virtual {v12, v2, v15}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 368
    .line 369
    .line 370
    iget-object v2, v11, Lf/ey1;->Q02:Lf/hz1;

    .line 371
    .line 372
    invoke-virtual {v2}, Lf/hz1;->toString()Ljava/lang/String;

    .line 373
    .line 374
    .line 375
    move-result-object v2

    .line 376
    invoke-virtual {v6, v2, v12}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 377
    .line 378
    .line 379
    add-int/lit8 v10, v10, 0x1

    .line 380
    .line 381
    move-object/from16 v7, v17

    .line 382
    .line 383
    move-object/from16 v5, v18

    .line 384
    .line 385
    move/from16 v8, v19

    .line 386
    .line 387
    move-object/from16 v2, v21

    .line 388
    .line 389
    goto/16 :goto_1d

    .line 390
    .line 391
    :catchall_186
    move-exception v0

    .line 392
    :try_start_187
    monitor-exit v11
    :try_end_188
    .catchall {:try_start_187 .. :try_end_188} :catchall_186

    .line 393
    throw v0

    .line 394
    :cond_189
    const/4 v1, 0x2

    .line 395
    invoke-virtual {v6, v1}, Lorg/json/JSONObject;->toString(I)Ljava/lang/String;

    .line 396
    .line 397
    .line 398
    move-result-object v1

    .line 399
    new-instance v2, Ljava/util/zip/ZipEntry;

    .line 400
    .line 401
    const-string v3, "data/encounter_tracker.json"

    .line 402
    .line 403
    invoke-direct {v2, v3}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 404
    .line 405
    .line 406
    invoke-virtual {v0, v2}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 407
    .line 408
    .line 409
    sget-object v2, Ljava/nio/charset/StandardCharsets;->UTF_8:Ljava/nio/charset/Charset;

    .line 410
    .line 411
    invoke-virtual {v1, v2}, Ljava/lang/String;->getBytes(Ljava/nio/charset/Charset;)[B

    .line 412
    .line 413
    .line 414
    move-result-object v1

    .line 415
    invoke-virtual {v0, v1}, Ljava/io/OutputStream;->write([B)V

    .line 416
    .line 417
    .line 418
    invoke-virtual {v0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 419
    .line 420
    .line 421
    return-void
.end method

.method public static El(Lf/n15;)V
    .registers 17

    .line 1
    move-object/from16 v0, p0

    .line 2
    .line 3
    const-string v1, "-m"

    .line 4
    .line 5
    const-string v2, "-f"

    .line 6
    .line 7
    filled-new-array {v1, v2}, [Ljava/lang/String;

    .line 8
    .line 9
    .line 10
    move-result-object v1

    .line 11
    const/4 v2, 0x0

    .line 12
    const/4 v3, 0x0

    .line 13
    :goto_c
    const/16 v4, 0x2c8

    .line 14
    .line 15
    if-ge v3, v4, :cond_97

    .line 16
    .line 17
    sget-object v4, Lf/pr;->ei0:Lf/rh6;

    .line 18
    .line 19
    invoke-static {v3}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 20
    .line 21
    .line 22
    move-result-object v5

    .line 23
    invoke-virtual {v4, v5, v2}, Lf/rh6;->Wm(Ljava/lang/Object;Z)Z

    .line 24
    .line 25
    .line 26
    move-result v4

    .line 27
    const/4 v5, 0x2

    .line 28
    const/4 v6, 0x1

    .line 29
    if-eqz v4, :cond_20

    .line 30
    .line 31
    const/4 v7, 0x2

    .line 32
    goto :goto_21

    .line 33
    :cond_20
    const/4 v7, 0x1

    .line 34
    :goto_21
    const/4 v8, 0x0

    .line 35
    :goto_22
    if-ge v8, v7, :cond_91

    .line 36
    .line 37
    new-array v9, v5, [Ljava/lang/Boolean;

    .line 38
    .line 39
    sget-object v10, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    .line 40
    .line 41
    aput-object v10, v9, v2

    .line 42
    .line 43
    sget-object v10, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;

    .line 44
    .line 45
    aput-object v10, v9, v6

    .line 46
    .line 47
    invoke-static {v9}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    .line 48
    .line 49
    .line 50
    move-result-object v9

    .line 51
    invoke-interface {v9}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    .line 52
    .line 53
    .line 54
    move-result-object v9

    .line 55
    :cond_36
    invoke-interface {v9}, Ljava/util/Iterator;->hasNext()Z

    .line 56
    .line 57
    .line 58
    move-result v10

    .line 59
    if-eqz v10, :cond_8b

    .line 60
    .line 61
    invoke-interface {v9}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 62
    .line 63
    .line 64
    move-result-object v10

    .line 65
    check-cast v10, Ljava/lang/Boolean;

    .line 66
    .line 67
    invoke-virtual {v10}, Ljava/lang/Boolean;->booleanValue()Z

    .line 68
    .line 69
    .line 70
    move-result v10

    .line 71
    sget-object v11, Lf/pr;->Sb1:Lf/pr;

    .line 72
    .line 73
    invoke-virtual {v11, v8, v3, v10, v2}, Lf/pr;->IL0(BSZZ)[Lf/wr2;

    .line 74
    .line 75
    .line 76
    move-result-object v11

    .line 77
    const/4 v12, 0x0

    .line 78
    :goto_4d
    array-length v13, v11

    .line 79
    if-ge v12, v13, :cond_36

    .line 80
    .line 81
    const-string v13, ""

    .line 82
    .line 83
    if-eqz v4, :cond_57

    .line 84
    .line 85
    aget-object v14, v1, v8

    .line 86
    .line 87
    goto :goto_58

    .line 88
    :cond_57
    move-object v14, v13

    .line 89
    :goto_58
    if-eqz v10, :cond_5c

    .line 90
    .line 91
    const-string v13, "-s"

    .line 92
    .line 93
    :cond_5c
    new-instance v15, Ljava/util/zip/ZipEntry;

    .line 94
    .line 95
    const-string v2, "sprites/monstericons/"

    .line 96
    .line 97
    const-string v5, "-"

    .line 98
    .line 99
    invoke-static {v2, v3, v5, v12, v14}, Lf/yn7;->q60(Ljava/lang/String;ILjava/lang/String;ILjava/lang/String;)Ljava/lang/StringBuilder;

    .line 100
    .line 101
    .line 102
    move-result-object v2

    .line 103
    const-string v5, ".png"

    .line 104
    .line 105
    invoke-static {v2, v13, v5}, Lf/jp3;->Cd(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 106
    .line 107
    .line 108
    move-result-object v2

    .line 109
    invoke-direct {v15, v2}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 110
    .line 111
    .line 112
    invoke-virtual {v0, v15}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 113
    .line 114
    .line 115
    aget-object v2, v11, v12

    .line 116
    .line 117
    invoke-virtual {v2}, Lf/wr2;->mL0()Lcom/badlogic/gdx/graphics/is0;

    .line 118
    .line 119
    .line 120
    move-result-object v2

    .line 121
    new-instance v5, Lf/u39;

    .line 122
    .line 123
    invoke-direct {v5, v0}, Lf/u39;-><init>(Ljava/io/OutputStream;)V

    .line 124
    .line 125
    .line 126
    invoke-static {v5, v2}, Lcom/badlogic/gdx/graphics/x6;->JZ(Lf/z46;Lcom/badlogic/gdx/graphics/is0;)V

    .line 127
    .line 128
    .line 129
    invoke-virtual {v2}, Lcom/badlogic/gdx/graphics/is0;->dispose()V

    .line 130
    .line 131
    .line 132
    invoke-virtual {v0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 133
    .line 134
    .line 135
    add-int/lit8 v12, v12, 0x1

    .line 136
    .line 137
    const/4 v2, 0x0

    .line 138
    const/4 v5, 0x2

    .line 139
    goto :goto_4d

    .line 140
    :cond_8b
    add-int/lit8 v8, v8, 0x1

    .line 141
    .line 142
    int-to-byte v8, v8

    .line 143
    const/4 v2, 0x0

    .line 144
    const/4 v5, 0x2

    .line 145
    goto :goto_22

    .line 146
    :cond_91
    add-int/lit8 v3, v3, 0x1

    .line 147
    .line 148
    int-to-short v3, v3

    .line 149
    const/4 v2, 0x0

    .line 150
    goto/16 :goto_c

    .line 151
    .line 152
    :cond_97
    return-void
.end method

.method public static Ja(Lf/n15;)V
    .registers 10

    .line 1
    sget-object v0, Lf/c21;->WT1:Lf/c21;

    .line 2
    .line 3
    iget-object v0, v0, Lf/c21;->vi1:Lf/ch4;

    .line 4
    .line 5
    iget v1, v0, Lf/ip8;->Mf1:I

    .line 6
    .line 7
    new-array v2, v1, [S

    .line 8
    .line 9
    iget-object v3, v0, Lf/pl6;->B91:[S

    .line 10
    .line 11
    iget-object v0, v0, Lf/o76;->QB:[B

    .line 12
    .line 13
    array-length v4, v3

    .line 14
    const/4 v5, 0x0

    .line 15
    const/4 v6, 0x0

    .line 16
    :goto_f
    add-int/lit8 v7, v4, -0x1

    .line 17
    .line 18
    if-lez v4, :cond_21

    .line 19
    .line 20
    aget-byte v4, v0, v7

    .line 21
    .line 22
    const/4 v8, 0x1

    .line 23
    if-ne v4, v8, :cond_1f

    .line 24
    .line 25
    add-int/lit8 v4, v6, 0x1

    .line 26
    .line 27
    aget-short v8, v3, v7

    .line 28
    .line 29
    aput-short v8, v2, v6

    .line 30
    .line 31
    move v6, v4

    .line 32
    :cond_1f
    move v4, v7

    .line 33
    goto :goto_f

    .line 34
    :cond_21
    const/4 v0, 0x0

    .line 35
    :goto_22
    if-ge v0, v1, :cond_51

    .line 36
    .line 37
    aget-short v3, v2, v0

    .line 38
    .line 39
    new-instance v4, Ljava/util/zip/ZipEntry;

    .line 40
    .line 41
    const-string v6, "sprites/itemicons/"

    .line 42
    .line 43
    const-string v7, ".png"

    .line 44
    .line 45
    invoke-static {v3, v6, v7}, Lf/yn7;->T6(ILjava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 46
    .line 47
    .line 48
    move-result-object v6

    .line 49
    invoke-direct {v4, v6}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 50
    .line 51
    .line 52
    invoke-virtual {p0, v4}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 53
    .line 54
    .line 55
    sget-object v4, Lf/c21;->WT1:Lf/c21;

    .line 56
    .line 57
    invoke-virtual {v4, v3, v5}, Lf/c21;->f51(SZ)Lf/qj6;

    .line 58
    .line 59
    .line 60
    move-result-object v3

    .line 61
    invoke-virtual {v3}, Lf/qj6;->RU0()Lcom/badlogic/gdx/graphics/is0;

    .line 62
    .line 63
    .line 64
    move-result-object v3

    .line 65
    new-instance v4, Lf/u39;

    .line 66
    .line 67
    invoke-direct {v4, p0}, Lf/u39;-><init>(Ljava/io/OutputStream;)V

    .line 68
    .line 69
    .line 70
    invoke-static {v4, v3}, Lcom/badlogic/gdx/graphics/x6;->JZ(Lf/z46;Lcom/badlogic/gdx/graphics/is0;)V

    .line 71
    .line 72
    .line 73
    invoke-virtual {v3}, Lcom/badlogic/gdx/graphics/is0;->dispose()V

    .line 74
    .line 75
    .line 76
    invoke-virtual {p0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 77
    .line 78
    .line 79
    add-int/lit8 v0, v0, 0x1

    .line 80
    .line 81
    goto :goto_22

    .line 82
    :cond_51
    return-void
.end method

.method public static KG0(Lf/n15;)V
    .registers 17

    .line 1
    move-object/from16 v0, p0

    .line 2
    .line 3
    const/4 v1, 0x2

    .line 4
    new-array v2, v1, [B

    .line 5
    .line 6
    fill-array-data v2, :array_10a

    .line 7
    .line 8
    .line 9
    const/4 v3, 0x0

    .line 10
    const/4 v4, 0x0

    .line 11
    :goto_a
    const-string v5, ".png"

    .line 12
    .line 13
    const-string v6, "-"

    .line 14
    .line 15
    if-ge v4, v1, :cond_70

    .line 16
    .line 17
    aget-byte v7, v2, v4

    .line 18
    .line 19
    sget-object v8, Lf/f2;->rt1:Lf/f2;

    .line 20
    .line 21
    iget-object v8, v8, Lf/f2;->Xf:Lf/k33;

    .line 22
    .line 23
    invoke-virtual {v8, v7}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 24
    .line 25
    .line 26
    move-result-object v8

    .line 27
    check-cast v8, Lf/l74;

    .line 28
    .line 29
    iget-object v8, v8, Lf/l74;->by1:Lf/k89;

    .line 30
    .line 31
    invoke-virtual {v8}, Lf/k89;->pj()[I

    .line 32
    .line 33
    .line 34
    move-result-object v8

    .line 35
    array-length v9, v8

    .line 36
    const/4 v10, 0x0

    .line 37
    :goto_24
    if-ge v10, v9, :cond_6c

    .line 38
    .line 39
    aget v11, v8, v10

    .line 40
    .line 41
    sget-object v12, Lf/f2;->rt1:Lf/f2;

    .line 42
    .line 43
    invoke-virtual {v12, v7, v11, v3}, Lf/f2;->bh0(BIZ)Lf/cc6;

    .line 44
    .line 45
    .line 46
    move-result-object v12

    .line 47
    const/4 v13, 0x0

    .line 48
    :goto_2f
    const/16 v14, 0x3e8

    .line 49
    .line 50
    if-ge v13, v14, :cond_68

    .line 51
    .line 52
    invoke-virtual {v12, v13}, Lf/cc6;->lP0(I)Z

    .line 53
    .line 54
    .line 55
    move-result v14

    .line 56
    if-nez v14, :cond_3a

    .line 57
    .line 58
    goto :goto_68

    .line 59
    :cond_3a
    new-instance v14, Ljava/util/zip/ZipEntry;

    .line 60
    .line 61
    const-string v15, "sprites/overworldsprites/"

    .line 62
    .line 63
    const-string v3, "/"

    .line 64
    .line 65
    invoke-static {v15, v7, v3, v11, v6}, Lf/yn7;->q60(Ljava/lang/String;ILjava/lang/String;ILjava/lang/String;)Ljava/lang/StringBuilder;

    .line 66
    .line 67
    .line 68
    move-result-object v3

    .line 69
    invoke-static {v3, v5, v13}, Lf/jp3;->gf(Ljava/lang/StringBuilder;Ljava/lang/String;I)Ljava/lang/String;

    .line 70
    .line 71
    .line 72
    move-result-object v3

    .line 73
    invoke-direct {v14, v3}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 74
    .line 75
    .line 76
    invoke-virtual {v0, v14}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 77
    .line 78
    .line 79
    invoke-virtual {v12, v13}, Lf/cc6;->p61(I)Lf/qj6;

    .line 80
    .line 81
    .line 82
    move-result-object v3

    .line 83
    invoke-virtual {v3}, Lf/qj6;->RU0()Lcom/badlogic/gdx/graphics/is0;

    .line 84
    .line 85
    .line 86
    move-result-object v3

    .line 87
    new-instance v14, Lf/u39;

    .line 88
    .line 89
    invoke-direct {v14, v0}, Lf/u39;-><init>(Ljava/io/OutputStream;)V

    .line 90
    .line 91
    .line 92
    invoke-static {v14, v3}, Lcom/badlogic/gdx/graphics/x6;->JZ(Lf/z46;Lcom/badlogic/gdx/graphics/is0;)V

    .line 93
    .line 94
    .line 95
    invoke-virtual {v3}, Lcom/badlogic/gdx/graphics/is0;->dispose()V

    .line 96
    .line 97
    .line 98
    invoke-virtual {v0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 99
    .line 100
    .line 101
    add-int/lit8 v13, v13, 0x1

    .line 102
    .line 103
    const/4 v3, 0x0

    .line 104
    goto :goto_2f

    .line 105
    :cond_68
    :goto_68
    add-int/lit8 v10, v10, 0x1

    .line 106
    .line 107
    const/4 v3, 0x0

    .line 108
    goto :goto_24

    .line 109
    :cond_6c
    add-int/lit8 v4, v4, 0x1

    .line 110
    .line 111
    const/4 v3, 0x0

    .line 112
    goto :goto_a

    .line 113
    :cond_70
    sget-object v2, Lf/p37;->T10:Lf/zw0;

    .line 114
    .line 115
    iget-object v2, v2, Lf/zw0;->pH1:Lf/v67;

    .line 116
    .line 117
    iget-object v2, v2, Lf/v67;->Bc:Lf/ch4;

    .line 118
    .line 119
    invoke-virtual {v2}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 120
    .line 121
    .line 122
    new-instance v3, Lf/u48;

    .line 123
    .line 124
    invoke-direct {v3, v2, v1}, Lf/u48;-><init>(Lf/o76;I)V

    .line 125
    .line 126
    .line 127
    invoke-virtual {v3}, Lf/u48;->iterator()Ljava/util/Iterator;

    .line 128
    .line 129
    .line 130
    move-result-object v1

    .line 131
    :cond_82
    :goto_82
    move-object v2, v1

    .line 132
    check-cast v2, Lf/rk3;

    .line 133
    .line 134
    invoke-virtual {v2}, Lf/rk3;->hasNext()Z

    .line 135
    .line 136
    .line 137
    move-result v2

    .line 138
    const/4 v3, 0x1

    .line 139
    if-eqz v2, :cond_e2

    .line 140
    .line 141
    move-object v2, v1

    .line 142
    check-cast v2, Lf/xx1;

    .line 143
    .line 144
    invoke-virtual {v2}, Lf/xx1;->next()Ljava/lang/Object;

    .line 145
    .line 146
    .line 147
    move-result-object v2

    .line 148
    check-cast v2, Lf/q06;

    .line 149
    .line 150
    iget-byte v4, v2, Lf/q06;->ue1:B

    .line 151
    .line 152
    if-ne v4, v3, :cond_82

    .line 153
    .line 154
    iget-object v4, v2, Lf/q06;->OA1:[Lf/qj6;

    .line 155
    .line 156
    array-length v7, v4

    .line 157
    if-ge v7, v3, :cond_9f

    .line 158
    .line 159
    goto :goto_82

    .line 160
    :cond_9f
    const/4 v3, 0x0

    .line 161
    :goto_a0
    array-length v7, v4

    .line 162
    if-ge v3, v7, :cond_82

    .line 163
    .line 164
    new-instance v7, Ljava/util/zip/ZipEntry;

    .line 165
    .line 166
    new-instance v8, Ljava/lang/StringBuilder;

    .line 167
    .line 168
    const-string v9, "sprites/overworldsprites/2/"

    .line 169
    .line 170
    invoke-direct {v8, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 171
    .line 172
    .line 173
    sget-object v9, Lf/p37;->T10:Lf/zw0;

    .line 174
    .line 175
    iget-object v9, v9, Lf/zw0;->pH1:Lf/v67;

    .line 176
    .line 177
    invoke-virtual {v9}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 178
    .line 179
    .line 180
    iget-short v9, v2, Lf/q06;->tt:S

    .line 181
    .line 182
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 183
    .line 184
    .line 185
    invoke-virtual {v8, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 186
    .line 187
    .line 188
    invoke-virtual {v8, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 189
    .line 190
    .line 191
    invoke-virtual {v8, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 192
    .line 193
    .line 194
    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 195
    .line 196
    .line 197
    move-result-object v8

    .line 198
    invoke-direct {v7, v8}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 199
    .line 200
    .line 201
    invoke-virtual {v0, v7}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 202
    .line 203
    .line 204
    aget-object v7, v4, v3

    .line 205
    .line 206
    invoke-virtual {v7}, Lf/qj6;->RU0()Lcom/badlogic/gdx/graphics/is0;

    .line 207
    .line 208
    .line 209
    move-result-object v7

    .line 210
    new-instance v8, Lf/u39;

    .line 211
    .line 212
    invoke-direct {v8, v0}, Lf/u39;-><init>(Ljava/io/OutputStream;)V

    .line 213
    .line 214
    .line 215
    invoke-static {v8, v7}, Lcom/badlogic/gdx/graphics/x6;->JZ(Lf/z46;Lcom/badlogic/gdx/graphics/is0;)V

    .line 216
    .line 217
    .line 218
    invoke-virtual {v7}, Lcom/badlogic/gdx/graphics/is0;->dispose()V

    .line 219
    .line 220
    .line 221
    invoke-virtual {v0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 222
    .line 223
    .line 224
    add-int/lit8 v3, v3, 0x1

    .line 225
    .line 226
    goto :goto_a0

    .line 227
    :cond_e2
    sget-object v1, Lf/p37;->T10:Lf/zw0;

    .line 228
    .line 229
    iget-object v1, v1, Lf/zw0;->lW0:Lf/l91;

    .line 230
    .line 231
    if-eqz v1, :cond_f6

    .line 232
    .line 233
    iget-object v1, v1, Lf/l91;->sI1:Lf/ch4;

    .line 234
    .line 235
    invoke-virtual {v1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 236
    .line 237
    .line 238
    new-instance v2, Lf/q49;

    .line 239
    .line 240
    const/4 v4, 0x0

    .line 241
    invoke-direct {v2, v0, v4}, Lf/q49;-><init>(Lf/n15;I)V

    .line 242
    .line 243
    .line 244
    invoke-virtual {v1, v2}, Lf/ch4;->HA(Lf/fc5;)Z

    .line 245
    .line 246
    .line 247
    :cond_f6
    sget-object v1, Lf/p37;->T10:Lf/zw0;

    .line 248
    .line 249
    iget-object v1, v1, Lf/zw0;->WP1:Lf/rf3;

    .line 250
    .line 251
    if-eqz v1, :cond_109

    .line 252
    .line 253
    iget-object v1, v1, Lf/rf3;->Va0:Lf/ch4;

    .line 254
    .line 255
    invoke-virtual {v1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 256
    .line 257
    .line 258
    new-instance v2, Lf/q49;

    .line 259
    .line 260
    invoke-direct {v2, v0, v3}, Lf/q49;-><init>(Lf/n15;I)V

    .line 261
    .line 262
    .line 263
    invoke-virtual {v1, v2}, Lf/ch4;->HA(Lf/fc5;)Z

    .line 264
    .line 265
    .line 266
    :cond_109
    return-void

    .line 267
    :array_10a
    .array-data 1
        0x0t
        0x1t
    .end array-data
.end method

.method public static Ut0(Lf/n15;)V
    .registers 11

    .line 1
    sget-object v0, Lf/p37;->se:Lf/qr3;

    .line 2
    .line 3
    if-nez v0, :cond_6

    .line 4
    .line 5
    goto/16 :goto_11b

    .line 6
    .line 7
    :cond_6
    sget-object v1, Lf/xe1;->fc1:Lf/xe1;

    .line 8
    .line 9
    invoke-virtual {v0, v1}, Lf/eb5;->QD1(Lf/xe1;)Lf/pd7;

    .line 10
    .line 11
    .line 12
    move-result-object v0

    .line 13
    sget-object v1, Lf/p37;->se:Lf/qr3;

    .line 14
    .line 15
    sget-object v2, Lf/xe1;->lD1:Lf/xe1;

    .line 16
    .line 17
    invoke-virtual {v1, v2}, Lf/eb5;->QD1(Lf/xe1;)Lf/pd7;

    .line 18
    .line 19
    .line 20
    move-result-object v1

    .line 21
    sget-object v2, Lf/p37;->se:Lf/qr3;

    .line 22
    .line 23
    sget-object v3, Lf/xe1;->p31:Lf/xe1;

    .line 24
    .line 25
    invoke-virtual {v2, v3}, Lf/eb5;->QD1(Lf/xe1;)Lf/pd7;

    .line 26
    .line 27
    .line 28
    move-result-object v2

    .line 29
    sget-object v3, Lf/p37;->se:Lf/qr3;

    .line 30
    .line 31
    iget-object v3, v3, Lf/eb5;->Bz1:Lf/yw3;

    .line 32
    .line 33
    if-eqz v1, :cond_11b

    .line 34
    .line 35
    if-eqz v2, :cond_11b

    .line 36
    .line 37
    if-nez v0, :cond_28

    .line 38
    .line 39
    goto/16 :goto_11b

    .line 40
    .line 41
    :cond_28
    new-instance v4, Lorg/json/JSONObject;

    .line 42
    .line 43
    invoke-direct {v4}, Lorg/json/JSONObject;-><init>()V

    .line 44
    .line 45
    .line 46
    new-instance v5, Lorg/json/JSONArray;

    .line 47
    .line 48
    invoke-direct {v5}, Lorg/json/JSONArray;-><init>()V

    .line 49
    .line 50
    .line 51
    invoke-virtual {v0}, Lf/pd7;->l81()[Lf/rd0;

    .line 52
    .line 53
    .line 54
    move-result-object v0

    .line 55
    array-length v6, v0

    .line 56
    const/4 v7, 0x0

    .line 57
    const/4 v8, 0x0

    .line 58
    :goto_39
    if-ge v8, v6, :cond_47

    .line 59
    .line 60
    aget-object v9, v0, v8

    .line 61
    .line 62
    invoke-static {v9, v7}, Lf/xf0;->qt(Lf/rd0;Z)Lorg/json/JSONObject;

    .line 63
    .line 64
    .line 65
    move-result-object v9

    .line 66
    invoke-virtual {v5, v9}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    .line 67
    .line 68
    .line 69
    add-int/lit8 v8, v8, 0x1

    .line 70
    .line 71
    goto :goto_39

    .line 72
    :cond_47
    const-string v0, "party"

    .line 73
    .line 74
    invoke-virtual {v4, v0, v5}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 75
    .line 76
    .line 77
    new-instance v0, Lorg/json/JSONArray;

    .line 78
    .line 79
    invoke-direct {v0}, Lorg/json/JSONArray;-><init>()V

    .line 80
    .line 81
    .line 82
    invoke-virtual {v1}, Lf/pd7;->l81()[Lf/rd0;

    .line 83
    .line 84
    .line 85
    move-result-object v1

    .line 86
    array-length v5, v1

    .line 87
    const/4 v6, 0x0

    .line 88
    :goto_57
    if-ge v6, v5, :cond_65

    .line 89
    .line 90
    aget-object v8, v1, v6

    .line 91
    .line 92
    invoke-static {v8, v7}, Lf/xf0;->qt(Lf/rd0;Z)Lorg/json/JSONObject;

    .line 93
    .line 94
    .line 95
    move-result-object v8

    .line 96
    invoke-virtual {v0, v8}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    .line 97
    .line 98
    .line 99
    add-int/lit8 v6, v6, 0x1

    .line 100
    .line 101
    goto :goto_57

    .line 102
    :cond_65
    const-string v1, "pc"

    .line 103
    .line 104
    invoke-virtual {v4, v1, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 105
    .line 106
    .line 107
    new-instance v1, Lorg/json/JSONArray;

    .line 108
    .line 109
    invoke-direct {v1}, Lorg/json/JSONArray;-><init>()V

    .line 110
    .line 111
    .line 112
    invoke-virtual {v2}, Lf/pd7;->l81()[Lf/rd0;

    .line 113
    .line 114
    .line 115
    move-result-object v2

    .line 116
    array-length v5, v2

    .line 117
    const/4 v6, 0x0

    .line 118
    :goto_75
    if-ge v6, v5, :cond_83

    .line 119
    .line 120
    aget-object v8, v2, v6

    .line 121
    .line 122
    invoke-static {v8, v7}, Lf/xf0;->qt(Lf/rd0;Z)Lorg/json/JSONObject;

    .line 123
    .line 124
    .line 125
    move-result-object v8

    .line 126
    invoke-virtual {v1, v8}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    .line 127
    .line 128
    .line 129
    add-int/lit8 v6, v6, 0x1

    .line 130
    .line 131
    goto :goto_75

    .line 132
    :cond_83
    const-string v1, "accountPC"

    .line 133
    .line 134
    invoke-virtual {v4, v1, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 135
    .line 136
    .line 137
    new-instance v0, Lorg/json/JSONObject;

    .line 138
    .line 139
    invoke-direct {v0}, Lorg/json/JSONObject;-><init>()V

    .line 140
    .line 141
    .line 142
    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 143
    .line 144
    .line 145
    sget-object v1, Lf/p37;->se:Lf/qr3;

    .line 146
    .line 147
    iget-object v1, v1, Lf/eb5;->BK0:Lf/eu6;

    .line 148
    .line 149
    iget-byte v1, v1, Lf/eu6;->Mb1:B

    .line 150
    .line 151
    invoke-static {v1}, Lf/ep8;->Fq(B)B

    .line 152
    .line 153
    .line 154
    move-result v1

    .line 155
    new-array v2, v1, [Lf/ep8;

    .line 156
    .line 157
    const/4 v5, 0x0

    .line 158
    :goto_9d
    if-ge v5, v1, :cond_a9

    .line 159
    .line 160
    invoke-virtual {v3, v5}, Lf/yw3;->Df(B)Lf/ep8;

    .line 161
    .line 162
    .line 163
    move-result-object v6

    .line 164
    aput-object v6, v2, v5

    .line 165
    .line 166
    add-int/lit8 v5, v5, 0x1

    .line 167
    .line 168
    int-to-byte v5, v5

    .line 169
    goto :goto_9d

    .line 170
    :cond_a9
    invoke-static {v2}, Lj$/util/DesugarArrays;->stream([Ljava/lang/Object;)Lj$/util/stream/Stream;

    .line 171
    .line 172
    .line 173
    move-result-object v1

    .line 174
    new-instance v2, Lf/ug6;

    .line 175
    .line 176
    const/4 v3, 0x5

    .line 177
    invoke-direct {v2, v3}, Lf/ug6;-><init>(I)V

    .line 178
    .line 179
    .line 180
    invoke-interface {v1, v2}, Lj$/util/stream/Stream;->filter(Ljava/util/function/Predicate;)Lj$/util/stream/Stream;

    .line 181
    .line 182
    .line 183
    move-result-object v1

    .line 184
    new-instance v2, Lf/ik0;

    .line 185
    .line 186
    invoke-direct {v2, v7}, Lf/ik0;-><init>(I)V

    .line 187
    .line 188
    .line 189
    invoke-interface {v1, v2}, Lj$/util/stream/Stream;->toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object;

    .line 190
    .line 191
    .line 192
    move-result-object v1

    .line 193
    check-cast v1, [Lf/ep8;

    .line 194
    .line 195
    array-length v2, v1

    .line 196
    :goto_c3
    if-ge v7, v2, :cond_fb

    .line 197
    .line 198
    aget-object v3, v1, v7

    .line 199
    .line 200
    new-instance v5, Lorg/json/JSONArray;

    .line 201
    .line 202
    invoke-direct {v5}, Lorg/json/JSONArray;-><init>()V

    .line 203
    .line 204
    .line 205
    iget-object v6, v3, Lf/ep8;->sy1:[Lf/nw0;

    .line 206
    .line 207
    invoke-static {v6}, Lj$/util/DesugarArrays;->stream([Ljava/lang/Object;)Lj$/util/stream/Stream;

    .line 208
    .line 209
    .line 210
    move-result-object v6

    .line 211
    new-instance v8, Lf/oc7;

    .line 212
    .line 213
    const/16 v9, 0x1d

    .line 214
    .line 215
    invoke-direct {v8, v9}, Lf/oc7;-><init>(I)V

    .line 216
    .line 217
    .line 218
    invoke-interface {v6, v8}, Lj$/util/stream/Stream;->map(Ljava/util/function/Function;)Lj$/util/stream/Stream;

    .line 219
    .line 220
    .line 221
    move-result-object v6

    .line 222
    new-instance v8, Lf/y55;

    .line 223
    .line 224
    const/16 v9, 0x12

    .line 225
    .line 226
    invoke-direct {v8, v9}, Lf/y55;-><init>(I)V

    .line 227
    .line 228
    .line 229
    invoke-interface {v6, v8}, Lj$/util/stream/Stream;->filter(Ljava/util/function/Predicate;)Lj$/util/stream/Stream;

    .line 230
    .line 231
    .line 232
    move-result-object v6

    .line 233
    new-instance v8, Lf/yv1;

    .line 234
    .line 235
    const/4 v9, 0x1

    .line 236
    invoke-direct {v8, v5, v9}, Lf/yv1;-><init>(Lorg/json/JSONArray;I)V

    .line 237
    .line 238
    .line 239
    invoke-interface {v6, v8}, Lj$/util/stream/Stream;->forEach(Ljava/util/function/Consumer;)V

    .line 240
    .line 241
    .line 242
    invoke-virtual {v3}, Lf/ep8;->Bl1()Ljava/lang/String;

    .line 243
    .line 244
    .line 245
    move-result-object v3

    .line 246
    invoke-virtual {v0, v3, v5}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 247
    .line 248
    .line 249
    add-int/lit8 v7, v7, 0x1

    .line 250
    .line 251
    goto :goto_c3

    .line 252
    :cond_fb
    const-string v1, "battleBox"

    .line 253
    .line 254
    invoke-virtual {v4, v1, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 255
    .line 256
    .line 257
    const/4 v0, 0x2

    .line 258
    invoke-virtual {v4, v0}, Lorg/json/JSONObject;->toString(I)Ljava/lang/String;

    .line 259
    .line 260
    .line 261
    move-result-object v0

    .line 262
    new-instance v1, Ljava/util/zip/ZipEntry;

    .line 263
    .line 264
    const-string v2, "data/monsters.json"

    .line 265
    .line 266
    invoke-direct {v1, v2}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 267
    .line 268
    .line 269
    invoke-virtual {p0, v1}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 270
    .line 271
    .line 272
    sget-object v1, Ljava/nio/charset/StandardCharsets;->UTF_8:Ljava/nio/charset/Charset;

    .line 273
    .line 274
    invoke-virtual {v0, v1}, Ljava/lang/String;->getBytes(Ljava/nio/charset/Charset;)[B

    .line 275
    .line 276
    .line 277
    move-result-object v0

    .line 278
    invoke-virtual {p0, v0}, Ljava/io/OutputStream;->write([B)V

    .line 279
    .line 280
    .line 281
    invoke-virtual {p0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 282
    .line 283
    .line 284
    :cond_11b
    :goto_11b
    return-void
.end method

.method public static W02(Lf/n15;Lf/ph7;Ljava/lang/String;)V
    .registers 12

    .line 1
    new-instance v0, Ljava/util/zip/ZipEntry;

    .line 2
    .line 3
    const-string v1, "sprites/battlesprites/"

    .line 4
    .line 5
    const-string v2, ".txt"

    .line 6
    .line 7
    invoke-static {v1, p2, v2}, Lf/cz7;->Ls0(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    .line 8
    .line 9
    .line 10
    move-result-object p2

    .line 11
    invoke-direct {v0, p2}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 12
    .line 13
    .line 14
    invoke-virtual {p0, v0}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 15
    .line 16
    .line 17
    new-instance p2, Ljava/io/PrintWriter;

    .line 18
    .line 19
    invoke-direct {p2, p0}, Ljava/io/PrintWriter;-><init>(Ljava/io/OutputStream;)V

    .line 20
    .line 21
    .line 22
    const-string v0, ";Table which determines scales for battle sprites.\r\n"

    .line 23
    .line 24
    invoke-virtual {p2, v0}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 25
    .line 26
    .line 27
    const-string v0, ";Lines starting with ; will be ignored\r\n"

    .line 28
    .line 29
    invoke-virtual {p2, v0}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 30
    .line 31
    .line 32
    const-string v0, ";Please only include values for overriden sprites!\r\n"

    .line 33
    .line 34
    invoke-virtual {p2, v0}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 35
    .line 36
    .line 37
    const-string v0, ";Each entry should be a separate line and contain ID=SCALE, like \"1=3\" without quotes.\r\n"

    .line 38
    .line 39
    invoke-virtual {p2, v0}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 40
    .line 41
    .line 42
    iget v0, p1, Lf/ip8;->Mf1:I

    .line 43
    .line 44
    new-array v1, v0, [S

    .line 45
    .line 46
    iget-object v2, p1, Lf/q61;->kG1:[S

    .line 47
    .line 48
    iget-object v3, p1, Lf/o76;->QB:[B

    .line 49
    .line 50
    array-length v4, v2

    .line 51
    const/4 v5, 0x0

    .line 52
    const/4 v6, 0x0

    .line 53
    :goto_34
    add-int/lit8 v7, v4, -0x1

    .line 54
    .line 55
    if-lez v4, :cond_46

    .line 56
    .line 57
    aget-byte v4, v3, v7

    .line 58
    .line 59
    const/4 v8, 0x1

    .line 60
    if-ne v4, v8, :cond_44

    .line 61
    .line 62
    add-int/lit8 v4, v6, 0x1

    .line 63
    .line 64
    aget-short v8, v2, v7

    .line 65
    .line 66
    aput-short v8, v1, v6

    .line 67
    .line 68
    move v6, v4

    .line 69
    :cond_44
    move v4, v7

    .line 70
    goto :goto_34

    .line 71
    :cond_46
    :goto_46
    if-ge v5, v0, :cond_6d

    .line 72
    .line 73
    aget-short v2, v1, v5

    .line 74
    .line 75
    new-instance v3, Ljava/lang/StringBuilder;

    .line 76
    .line 77
    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    .line 78
    .line 79
    .line 80
    invoke-virtual {v3, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 81
    .line 82
    .line 83
    const-string v4, "="

    .line 84
    .line 85
    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 86
    .line 87
    .line 88
    invoke-virtual {p1, v2}, Lf/ph7;->F50(S)F

    .line 89
    .line 90
    .line 91
    move-result v2

    .line 92
    invoke-virtual {v3, v2}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;

    .line 93
    .line 94
    .line 95
    const-string v2, "\r\n"

    .line 96
    .line 97
    invoke-virtual {v3, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 98
    .line 99
    .line 100
    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 101
    .line 102
    .line 103
    move-result-object v2

    .line 104
    invoke-virtual {p2, v2}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 105
    .line 106
    .line 107
    add-int/lit8 v5, v5, 0x1

    .line 108
    .line 109
    goto :goto_46

    .line 110
    :cond_6d
    invoke-virtual {p2}, Ljava/io/PrintWriter;->flush()V

    .line 111
    .line 112
    .line 113
    invoke-virtual {p0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 114
    .line 115
    .line 116
    return-void
.end method

.method public static YL(Lf/n15;)V
    .registers 20

    .line 1
    move-object/from16 v0, p0

    .line 2
    .line 3
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 4
    .line 5
    .line 6
    move-result-object v1

    .line 7
    iget-object v1, v1, Lf/y91;->Uv:Ljava/util/HashMap;

    .line 8
    .line 9
    invoke-virtual {v1}, Ljava/util/HashMap;->values()Ljava/util/Collection;

    .line 10
    .line 11
    .line 12
    move-result-object v1

    .line 13
    invoke-interface {v1}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    .line 14
    .line 15
    .line 16
    move-result-object v1

    .line 17
    :cond_10
    :goto_10
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    .line 18
    .line 19
    .line 20
    move-result v2

    .line 21
    const-string v3, "front"

    .line 22
    .line 23
    const-string v4, "back"

    .line 24
    .line 25
    const/4 v5, 0x2

    .line 26
    const/4 v6, 0x0

    .line 27
    const/4 v7, 0x1

    .line 28
    if-eqz v2, :cond_12a

    .line 29
    .line 30
    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 31
    .line 32
    .line 33
    move-result-object v2

    .line 34
    check-cast v2, Lf/zp3;

    .line 35
    .line 36
    iget-short v8, v2, Lf/zp3;->Kj1:S

    .line 37
    .line 38
    iget-object v9, v2, Lf/zp3;->wd1:Lf/wq3;

    .line 39
    .line 40
    sget-object v10, Lf/wq3;->UK1:Lf/wq3;

    .line 41
    .line 42
    if-ne v9, v10, :cond_2c

    .line 43
    .line 44
    goto :goto_10

    .line 45
    :cond_2c
    iget-short v2, v2, Lf/zp3;->VF0:S

    .line 46
    .line 47
    const/16 v9, 0xfe

    .line 48
    .line 49
    if-lez v2, :cond_3b

    .line 50
    .line 51
    if-ge v2, v9, :cond_3b

    .line 52
    .line 53
    new-array v2, v5, [B

    .line 54
    .line 55
    aput-byte v6, v2, v6

    .line 56
    .line 57
    aput-byte v7, v2, v7

    .line 58
    .line 59
    goto :goto_46

    .line 60
    :cond_3b
    if-ne v2, v9, :cond_42

    .line 61
    .line 62
    new-array v2, v7, [B

    .line 63
    .line 64
    aput-byte v7, v2, v6

    .line 65
    .line 66
    goto :goto_46

    .line 67
    :cond_42
    new-array v2, v7, [B

    .line 68
    .line 69
    aput-byte v6, v2, v6

    .line 70
    .line 71
    :goto_46
    const/4 v9, 0x0

    .line 72
    :goto_47
    if-ge v9, v5, :cond_10

    .line 73
    .line 74
    const/4 v10, 0x0

    .line 75
    :goto_4a
    if-ge v10, v5, :cond_11f

    .line 76
    .line 77
    new-instance v11, Ljava/util/ArrayList;

    .line 78
    .line 79
    invoke-direct {v11}, Ljava/util/ArrayList;-><init>()V

    .line 80
    .line 81
    .line 82
    array-length v12, v2

    .line 83
    const/4 v13, 0x0

    .line 84
    :goto_53
    if-ge v13, v12, :cond_ad

    .line 85
    .line 86
    aget-byte v14, v2, v13

    .line 87
    .line 88
    sget-object v15, Lf/pr;->Sb1:Lf/pr;

    .line 89
    .line 90
    if-ne v9, v7, :cond_5c

    .line 91
    .line 92
    const/4 v6, 0x1

    .line 93
    :cond_5c
    const/16 v16, 0x0

    .line 94
    .line 95
    if-ne v10, v7, :cond_64

    .line 96
    .line 97
    const/4 v5, 0x1

    .line 98
    :goto_61
    const/16 v17, 0x2

    .line 99
    .line 100
    goto :goto_66

    .line 101
    :cond_64
    const/4 v5, 0x0

    .line 102
    goto :goto_61

    .line 103
    :goto_66
    invoke-virtual {v15, v14, v8, v6, v5}, Lf/pr;->fU0(BSZZ)Z

    .line 104
    .line 105
    .line 106
    move-result v5

    .line 107
    if-eqz v5, :cond_6d

    .line 108
    .line 109
    goto :goto_a7

    .line 110
    :cond_6d
    if-ne v9, v7, :cond_71

    .line 111
    .line 112
    const/4 v5, 0x1

    .line 113
    goto :goto_72

    .line 114
    :cond_71
    const/4 v5, 0x0

    .line 115
    :goto_72
    if-ne v10, v7, :cond_76

    .line 116
    .line 117
    const/4 v6, 0x1

    .line 118
    goto :goto_77

    .line 119
    :cond_76
    const/4 v6, 0x0

    .line 120
    :goto_77
    invoke-virtual {v15, v14, v8, v5, v6}, Lf/pr;->Uw(BSZZ)[Lf/wr2;

    .line 121
    .line 122
    .line 123
    move-result-object v5

    .line 124
    aget-object v5, v5, v16

    .line 125
    .line 126
    invoke-virtual {v5}, Lf/wr2;->mL0()Lcom/badlogic/gdx/graphics/is0;

    .line 127
    .line 128
    .line 129
    move-result-object v5

    .line 130
    invoke-virtual {v11}, Ljava/util/ArrayList;->size()I

    .line 131
    .line 132
    .line 133
    move-result v6

    .line 134
    const/4 v14, 0x0

    .line 135
    :goto_86
    if-ge v14, v6, :cond_a4

    .line 136
    .line 137
    invoke-virtual {v11, v14}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 138
    .line 139
    .line 140
    move-result-object v15

    .line 141
    add-int/lit8 v14, v14, 0x1

    .line 142
    .line 143
    check-cast v15, Lcom/badlogic/gdx/graphics/is0;

    .line 144
    .line 145
    invoke-virtual {v5}, Lcom/badlogic/gdx/graphics/is0;->db0()Ljava/nio/ByteBuffer;

    .line 146
    .line 147
    .line 148
    move-result-object v7

    .line 149
    invoke-virtual {v15}, Lcom/badlogic/gdx/graphics/is0;->db0()Ljava/nio/ByteBuffer;

    .line 150
    .line 151
    .line 152
    move-result-object v15

    .line 153
    invoke-virtual {v7, v15}, Ljava/nio/ByteBuffer;->equals(Ljava/lang/Object;)Z

    .line 154
    .line 155
    .line 156
    move-result v7

    .line 157
    if-eqz v7, :cond_a2

    .line 158
    .line 159
    invoke-virtual {v5}, Lcom/badlogic/gdx/graphics/is0;->dispose()V

    .line 160
    .line 161
    .line 162
    goto :goto_a7

    .line 163
    :cond_a2
    const/4 v7, 0x1

    .line 164
    goto :goto_86

    .line 165
    :cond_a4
    invoke-virtual {v11, v5}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 166
    .line 167
    .line 168
    :goto_a7
    add-int/lit8 v13, v13, 0x1

    .line 169
    .line 170
    const/4 v5, 0x2

    .line 171
    const/4 v6, 0x0

    .line 172
    const/4 v7, 0x1

    .line 173
    goto :goto_53

    .line 174
    :cond_ad
    const/16 v16, 0x0

    .line 175
    .line 176
    const/16 v17, 0x2

    .line 177
    .line 178
    invoke-virtual {v11}, Ljava/util/ArrayList;->size()I

    .line 179
    .line 180
    .line 181
    move-result v5

    .line 182
    const/4 v6, 0x1

    .line 183
    if-le v5, v6, :cond_ba

    .line 184
    .line 185
    const/4 v6, 0x1

    .line 186
    goto :goto_bb

    .line 187
    :cond_ba
    const/4 v6, 0x0

    .line 188
    :goto_bb
    const/4 v5, 0x0

    .line 189
    :goto_bc
    invoke-virtual {v11}, Ljava/util/ArrayList;->size()I

    .line 190
    .line 191
    .line 192
    move-result v7

    .line 193
    if-ge v5, v7, :cond_118

    .line 194
    .line 195
    if-eqz v6, :cond_cc

    .line 196
    .line 197
    if-nez v5, :cond_c9

    .line 198
    .line 199
    const-string v7, "-m"

    .line 200
    .line 201
    goto :goto_ce

    .line 202
    :cond_c9
    const-string v7, "-f"

    .line 203
    .line 204
    goto :goto_ce

    .line 205
    :cond_cc
    const-string v7, ""

    .line 206
    .line 207
    :goto_ce
    new-instance v12, Ljava/util/zip/ZipEntry;

    .line 208
    .line 209
    const-string v13, "sprites/battlesprites/"

    .line 210
    .line 211
    const-string v14, "-"

    .line 212
    .line 213
    invoke-static {v8, v13, v14}, Lf/jp3;->Tt1(ILjava/lang/String;Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 214
    .line 215
    .line 216
    move-result-object v13

    .line 217
    const/4 v15, 0x1

    .line 218
    if-ne v9, v15, :cond_dd

    .line 219
    .line 220
    move-object v15, v4

    .line 221
    goto :goto_de

    .line 222
    :cond_dd
    move-object v15, v3

    .line 223
    :goto_de
    invoke-virtual {v13, v15}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 224
    .line 225
    .line 226
    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 227
    .line 228
    .line 229
    const/4 v15, 0x1

    .line 230
    if-ne v10, v15, :cond_ea

    .line 231
    .line 232
    const-string v14, "s"

    .line 233
    .line 234
    goto :goto_ec

    .line 235
    :cond_ea
    const-string v14, "n"

    .line 236
    .line 237
    :goto_ec
    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 238
    .line 239
    .line 240
    invoke-virtual {v13, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 241
    .line 242
    .line 243
    const-string v7, ".png"

    .line 244
    .line 245
    invoke-virtual {v13, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 246
    .line 247
    .line 248
    invoke-virtual {v13}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 249
    .line 250
    .line 251
    move-result-object v7

    .line 252
    invoke-direct {v12, v7}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 253
    .line 254
    .line 255
    invoke-virtual {v0, v12}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 256
    .line 257
    .line 258
    invoke-virtual {v11, v5}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 259
    .line 260
    .line 261
    move-result-object v7

    .line 262
    check-cast v7, Lcom/badlogic/gdx/graphics/is0;

    .line 263
    .line 264
    new-instance v12, Lf/u39;

    .line 265
    .line 266
    invoke-direct {v12, v0}, Lf/u39;-><init>(Ljava/io/OutputStream;)V

    .line 267
    .line 268
    .line 269
    invoke-static {v12, v7}, Lcom/badlogic/gdx/graphics/x6;->JZ(Lf/z46;Lcom/badlogic/gdx/graphics/is0;)V

    .line 270
    .line 271
    .line 272
    invoke-virtual {v7}, Lcom/badlogic/gdx/graphics/is0;->dispose()V

    .line 273
    .line 274
    .line 275
    invoke-virtual {v0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 276
    .line 277
    .line 278
    add-int/lit8 v5, v5, 0x1

    .line 279
    .line 280
    goto :goto_bc

    .line 281
    :cond_118
    add-int/lit8 v10, v10, 0x1

    .line 282
    .line 283
    const/4 v5, 0x2

    .line 284
    const/4 v6, 0x0

    .line 285
    const/4 v7, 0x1

    .line 286
    goto/16 :goto_4a

    .line 287
    .line 288
    :cond_11f
    const/16 v16, 0x0

    .line 289
    .line 290
    const/16 v17, 0x2

    .line 291
    .line 292
    add-int/lit8 v9, v9, 0x1

    .line 293
    .line 294
    const/4 v5, 0x2

    .line 295
    const/4 v6, 0x0

    .line 296
    const/4 v7, 0x1

    .line 297
    goto/16 :goto_47

    .line 298
    .line 299
    :cond_12a
    const/16 v16, 0x0

    .line 300
    .line 301
    const/16 v17, 0x2

    .line 302
    .line 303
    sget-object v1, Lf/pr;->Sb1:Lf/pr;

    .line 304
    .line 305
    iget-object v2, v1, Lf/pr;->wg1:[Lf/ph7;

    .line 306
    .line 307
    iget-object v5, v1, Lf/pr;->wg1:[Lf/ph7;

    .line 308
    .line 309
    aget-object v2, v2, v16

    .line 310
    .line 311
    if-nez v2, :cond_13d

    .line 312
    .line 313
    new-instance v2, Lf/ph7;

    .line 314
    .line 315
    invoke-direct {v2}, Lf/ph7;-><init>()V

    .line 316
    .line 317
    .line 318
    :cond_13d
    const-string v6, "table-front-scale"

    .line 319
    .line 320
    invoke-static {v0, v2, v6}, Lf/xf0;->W02(Lf/n15;Lf/ph7;Ljava/lang/String;)V

    .line 321
    .line 322
    .line 323
    const/16 v18, 0x1

    .line 324
    .line 325
    aget-object v2, v5, v18

    .line 326
    .line 327
    if-nez v2, :cond_14d

    .line 328
    .line 329
    new-instance v2, Lf/ph7;

    .line 330
    .line 331
    invoke-direct {v2}, Lf/ph7;-><init>()V

    .line 332
    .line 333
    .line 334
    :cond_14d
    const-string v6, "table-back-scale"

    .line 335
    .line 336
    invoke-static {v0, v2, v6}, Lf/xf0;->W02(Lf/n15;Lf/ph7;Ljava/lang/String;)V

    .line 337
    .line 338
    .line 339
    aget-object v2, v5, v17

    .line 340
    .line 341
    if-nez v2, :cond_15b

    .line 342
    .line 343
    new-instance v2, Lf/ph7;

    .line 344
    .line 345
    invoke-direct {v2}, Lf/ph7;-><init>()V

    .line 346
    .line 347
    .line 348
    :cond_15b
    const-string v5, "table-summary-scale"

    .line 349
    .line 350
    invoke-static {v0, v2, v5}, Lf/xf0;->W02(Lf/n15;Lf/ph7;Ljava/lang/String;)V

    .line 351
    .line 352
    .line 353
    iget-object v1, v1, Lf/pr;->F31:Lf/k89;

    .line 354
    .line 355
    if-nez v1, :cond_169

    .line 356
    .line 357
    new-instance v1, Lf/k89;

    .line 358
    .line 359
    invoke-direct {v1}, Lf/x44;-><init>()V

    .line 360
    .line 361
    .line 362
    :cond_169
    new-instance v1, Ljava/util/zip/ZipEntry;

    .line 363
    .line 364
    const-string v2, "sprites/battlesprites/table-coordinate-mods.txt"

    .line 365
    .line 366
    invoke-direct {v1, v2}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 367
    .line 368
    .line 369
    invoke-virtual {v0, v1}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 370
    .line 371
    .line 372
    new-instance v1, Ljava/io/PrintWriter;

    .line 373
    .line 374
    invoke-direct {v1, v0}, Ljava/io/PrintWriter;-><init>(Ljava/io/OutputStream;)V

    .line 375
    .line 376
    .line 377
    const-string v2, ";Table which determines coordinate modifications for battle sprites.\r\n"

    .line 378
    .line 379
    invoke-virtual {v1, v2}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 380
    .line 381
    .line 382
    const-string v2, ";Lines starting with ; will be ignored\r\n"

    .line 383
    .line 384
    invoke-virtual {v1, v2}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 385
    .line 386
    .line 387
    const-string v2, ";Please only include values for overriden sprites!\r\n"

    .line 388
    .line 389
    invoke-virtual {v1, v2}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 390
    .line 391
    .line 392
    const-string v2, ";Each entry should be a separate line and contain ID,(FRONT/BACK)=X,Y,Z. Scale is clamped from -1 to 1. Default values for all fields are 0.\r\n"

    .line 393
    .line 394
    invoke-virtual {v1, v2}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 395
    .line 396
    .line 397
    const-string v2, ";X: Negative values push left, positive values push right.\r\n"

    .line 398
    .line 399
    invoke-virtual {v1, v2}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 400
    .line 401
    .line 402
    const-string v2, ";Y: Higher values push up, lower values push down.\r\n"

    .line 403
    .line 404
    invoke-virtual {v1, v2}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 405
    .line 406
    .line 407
    const-string v2, ";Z: Higher values push away from the camera, lower values push towards the camera.\r\n"

    .line 408
    .line 409
    invoke-virtual {v1, v2}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 410
    .line 411
    .line 412
    const-string v2, ";Scale is clamped from -1 to 1.\r\n"

    .line 413
    .line 414
    invoke-virtual {v1, v2}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 415
    .line 416
    .line 417
    const-string v2, ";Example (Altitude mod only, increasing Y by 0.31): 1,front=0,0.31,0\r\n"

    .line 418
    .line 419
    invoke-virtual {v1, v2}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 420
    .line 421
    .line 422
    const/4 v2, 0x2

    .line 423
    new-array v5, v2, [Z

    .line 424
    .line 425
    fill-array-data v5, :array_224

    .line 426
    .line 427
    .line 428
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 429
    .line 430
    .line 431
    move-result-object v2

    .line 432
    iget-object v2, v2, Lf/y91;->Uv:Ljava/util/HashMap;

    .line 433
    .line 434
    invoke-virtual {v2}, Ljava/util/HashMap;->values()Ljava/util/Collection;

    .line 435
    .line 436
    .line 437
    move-result-object v2

    .line 438
    invoke-interface {v2}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    .line 439
    .line 440
    .line 441
    move-result-object v2

    .line 442
    :cond_1b9
    :goto_1b9
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    .line 443
    .line 444
    .line 445
    move-result v6

    .line 446
    if-eqz v6, :cond_21c

    .line 447
    .line 448
    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 449
    .line 450
    .line 451
    move-result-object v6

    .line 452
    check-cast v6, Lf/zp3;

    .line 453
    .line 454
    iget-object v7, v6, Lf/zp3;->wd1:Lf/wq3;

    .line 455
    .line 456
    sget-object v8, Lf/wq3;->UK1:Lf/wq3;

    .line 457
    .line 458
    if-ne v7, v8, :cond_1cc

    .line 459
    .line 460
    goto :goto_1b9

    .line 461
    :cond_1cc
    iget-short v6, v6, Lf/zp3;->Kj1:S

    .line 462
    .line 463
    const/4 v7, 0x0

    .line 464
    const/4 v8, 0x2

    .line 465
    :goto_1d0
    if-ge v7, v8, :cond_1b9

    .line 466
    .line 467
    aget-boolean v9, v5, v7

    .line 468
    .line 469
    sget-object v10, Lf/pr;->Sb1:Lf/pr;

    .line 470
    .line 471
    invoke-virtual {v10, v6, v9}, Lf/pr;->V50(SZ)Lf/u07;

    .line 472
    .line 473
    .line 474
    move-result-object v10

    .line 475
    sget-object v11, Lf/pr;->UR0:Lf/u07;

    .line 476
    .line 477
    if-eq v10, v11, :cond_219

    .line 478
    .line 479
    new-instance v11, Ljava/lang/StringBuilder;

    .line 480
    .line 481
    invoke-direct {v11}, Ljava/lang/StringBuilder;-><init>()V

    .line 482
    .line 483
    .line 484
    invoke-virtual {v11, v6}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 485
    .line 486
    .line 487
    const-string v12, ","

    .line 488
    .line 489
    invoke-virtual {v11, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 490
    .line 491
    .line 492
    if-eqz v9, :cond_1ef

    .line 493
    .line 494
    move-object v9, v4

    .line 495
    goto :goto_1f0

    .line 496
    :cond_1ef
    move-object v9, v3

    .line 497
    :goto_1f0
    invoke-virtual {v11, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 498
    .line 499
    .line 500
    const-string v9, "="

    .line 501
    .line 502
    invoke-virtual {v11, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 503
    .line 504
    .line 505
    iget v9, v10, Lf/u07;->x:F

    .line 506
    .line 507
    invoke-virtual {v11, v9}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;

    .line 508
    .line 509
    .line 510
    invoke-virtual {v11, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 511
    .line 512
    .line 513
    iget v9, v10, Lf/u07;->y:F

    .line 514
    .line 515
    invoke-virtual {v11, v9}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;

    .line 516
    .line 517
    .line 518
    invoke-virtual {v11, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 519
    .line 520
    .line 521
    iget v9, v10, Lf/u07;->z:F

    .line 522
    .line 523
    invoke-virtual {v11, v9}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;

    .line 524
    .line 525
    .line 526
    const-string v9, "\r\n"

    .line 527
    .line 528
    invoke-virtual {v11, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 529
    .line 530
    .line 531
    invoke-virtual {v11}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 532
    .line 533
    .line 534
    move-result-object v9

    .line 535
    invoke-virtual {v1, v9}, Ljava/io/PrintWriter;->write(Ljava/lang/String;)V

    .line 536
    .line 537
    .line 538
    :cond_219
    add-int/lit8 v7, v7, 0x1

    .line 539
    .line 540
    goto :goto_1d0

    .line 541
    :cond_21c
    invoke-virtual {v1}, Ljava/io/PrintWriter;->flush()V

    .line 542
    .line 543
    .line 544
    invoke-virtual {v0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 545
    .line 546
    .line 547
    return-void

    .line 548
    nop

    .line 549
    :array_224
    .array-data 1
        0x0t
        0x1t
    .end array-data
.end method

.method public static YR0(Lf/n15;)V
    .registers 14

    .line 1
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 2
    .line 3
    .line 4
    move-result-object v0

    .line 5
    iget-object v0, v0, Lf/y91;->Uv:Ljava/util/HashMap;

    .line 6
    .line 7
    invoke-virtual {v0}, Ljava/util/HashMap;->values()Ljava/util/Collection;

    .line 8
    .line 9
    .line 10
    move-result-object v0

    .line 11
    invoke-interface {v0}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    .line 12
    .line 13
    .line 14
    move-result-object v0

    .line 15
    :cond_e
    :goto_e
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    .line 16
    .line 17
    .line 18
    move-result v1

    .line 19
    if-eqz v1, :cond_188

    .line 20
    .line 21
    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 22
    .line 23
    .line 24
    move-result-object v1

    .line 25
    check-cast v1, Lf/zp3;

    .line 26
    .line 27
    iget-short v2, v1, Lf/zp3;->Kj1:S

    .line 28
    .line 29
    const/4 v3, 0x1

    .line 30
    if-lt v2, v3, :cond_e

    .line 31
    .line 32
    const/16 v4, 0x289

    .line 33
    .line 34
    if-le v2, v4, :cond_24

    .line 35
    .line 36
    goto :goto_e

    .line 37
    :cond_24
    sget-object v4, Lf/vh7;->qi1:Lf/ch4;

    .line 38
    .line 39
    invoke-virtual {v4, v2}, Lf/pl6;->ZK1(S)Z

    .line 40
    .line 41
    .line 42
    move-result v5

    .line 43
    const/4 v6, 0x3

    .line 44
    const/4 v7, 0x2

    .line 45
    const/4 v8, 0x0

    .line 46
    const/4 v9, 0x0

    .line 47
    if-eqz v5, :cond_60

    .line 48
    .line 49
    invoke-virtual {v4, v2}, Lf/ch4;->xH0(S)Ljava/lang/Object;

    .line 50
    .line 51
    .line 52
    move-result-object v4

    .line 53
    check-cast v4, Lf/xg7;

    .line 54
    .line 55
    iget-object v4, v4, Lf/ix4;->N0:Lf/z46;

    .line 56
    .line 57
    invoke-virtual {v4}, Lf/z46;->yD()[B

    .line 58
    .line 59
    .line 60
    move-result-object v4

    .line 61
    if-eqz v4, :cond_5a

    .line 62
    .line 63
    array-length v5, v4

    .line 64
    const/4 v10, 0x5

    .line 65
    if-ge v5, v10, :cond_43

    .line 66
    .line 67
    goto :goto_5a

    .line 68
    :cond_43
    aget-byte v5, v4, v8

    .line 69
    .line 70
    const/16 v10, 0x52

    .line 71
    .line 72
    if-ne v5, v10, :cond_5a

    .line 73
    .line 74
    aget-byte v5, v4, v3

    .line 75
    .line 76
    const/16 v10, 0x49

    .line 77
    .line 78
    if-ne v5, v10, :cond_5a

    .line 79
    .line 80
    aget-byte v5, v4, v7

    .line 81
    .line 82
    const/16 v10, 0x46

    .line 83
    .line 84
    if-ne v5, v10, :cond_5a

    .line 85
    .line 86
    aget-byte v5, v4, v6

    .line 87
    .line 88
    if-ne v5, v10, :cond_5a

    .line 89
    .line 90
    goto :goto_5b

    .line 91
    :cond_5a
    :goto_5a
    move-object v4, v9

    .line 92
    :goto_5b
    if-eqz v4, :cond_60

    .line 93
    .line 94
    move-object v9, v4

    .line 95
    goto/16 :goto_162

    .line 96
    .line 97
    :cond_60
    sget-object v4, Lf/p37;->T10:Lf/zw0;

    .line 98
    .line 99
    iget-object v4, v4, Lf/zw0;->pH1:Lf/v67;

    .line 100
    .line 101
    invoke-virtual {v4}, Lf/sg7;->Tc1()Lf/fn8;

    .line 102
    .line 103
    .line 104
    move-result-object v4

    .line 105
    iget-object v5, v4, Lf/fn8;->O3:Lf/gc2;

    .line 106
    .line 107
    if-ltz v2, :cond_76

    .line 108
    .line 109
    iget-object v10, v5, Lf/gc2;->sz:[Ljava/lang/Object;

    .line 110
    .line 111
    check-cast v10, [Lf/oi1;

    .line 112
    .line 113
    aget-object v10, v10, v6

    .line 114
    .line 115
    iget v10, v10, Lf/oi1;->vz:I

    .line 116
    .line 117
    if-lt v2, v10, :cond_77

    .line 118
    .line 119
    :cond_76
    const/4 v2, 0x1

    .line 120
    :cond_77
    iget-object v5, v5, Lf/gc2;->sz:[Ljava/lang/Object;

    .line 121
    .line 122
    check-cast v5, [Lf/oi1;

    .line 123
    .line 124
    aget-object v5, v5, v6

    .line 125
    .line 126
    iget-object v5, v5, Lf/oi1;->mU:Ljava/lang/Object;

    .line 127
    .line 128
    check-cast v5, [Lf/m89;

    .line 129
    .line 130
    aget-object v2, v5, v2

    .line 131
    .line 132
    check-cast v2, Lf/s0;

    .line 133
    .line 134
    iget-short v2, v2, Lf/s0;->C41:S

    .line 135
    .line 136
    new-instance v5, Lf/er7;

    .line 137
    .line 138
    iget-object v4, v4, Lf/fn8;->y7:Lf/kl8;

    .line 139
    .line 140
    iget-object v4, v4, Lf/kl8;->Lpt9:Ljava/lang/Object;

    .line 141
    .line 142
    check-cast v4, [Lf/l19;

    .line 143
    .line 144
    aget-object v2, v4, v2

    .line 145
    .line 146
    invoke-direct {v5, v2}, Lf/er7;-><init>(Lf/l19;)V

    .line 147
    .line 148
    .line 149
    invoke-virtual {v5}, Lf/er7;->Pi0()Lf/y52;

    .line 150
    .line 151
    .line 152
    move-result-object v2

    .line 153
    if-nez v2, :cond_9c

    .line 154
    .line 155
    goto/16 :goto_162

    .line 156
    .line 157
    :cond_9c
    iget v4, v2, Lf/y52;->Ns1:I

    .line 158
    .line 159
    iget-byte v5, v2, Lf/y52;->CM0:B

    .line 160
    .line 161
    iget v6, v2, Lf/y52;->zk1:I

    .line 162
    .line 163
    iget v9, v2, Lf/y52;->up:I

    .line 164
    .line 165
    const/16 v10, 0x10

    .line 166
    .line 167
    if-nez v5, :cond_ac

    .line 168
    .line 169
    mul-int/lit8 v9, v9, 0x2

    .line 170
    .line 171
    const/16 v6, 0x10

    .line 172
    .line 173
    :cond_ac
    add-int/lit8 v11, v9, 0x2c

    .line 174
    .line 175
    invoke-static {v11}, Ljava/nio/ByteBuffer;->allocate(I)Ljava/nio/ByteBuffer;

    .line 176
    .line 177
    .line 178
    move-result-object v11

    .line 179
    sget-object v12, Ljava/nio/ByteOrder;->LITTLE_ENDIAN:Ljava/nio/ByteOrder;

    .line 180
    .line 181
    invoke-virtual {v11, v12}, Ljava/nio/ByteBuffer;->order(Ljava/nio/ByteOrder;)Ljava/nio/ByteBuffer;

    .line 182
    .line 183
    .line 184
    move-result-object v11

    .line 185
    const v12, 0x46464952

    .line 186
    .line 187
    .line 188
    invoke-virtual {v11, v12}, Ljava/nio/ByteBuffer;->putInt(I)Ljava/nio/ByteBuffer;

    .line 189
    .line 190
    .line 191
    add-int/lit8 v12, v9, 0x24

    .line 192
    .line 193
    invoke-virtual {v11, v12}, Ljava/nio/ByteBuffer;->putInt(I)Ljava/nio/ByteBuffer;

    .line 194
    .line 195
    .line 196
    const v12, 0x45564157

    .line 197
    .line 198
    .line 199
    invoke-virtual {v11, v12}, Ljava/nio/ByteBuffer;->putInt(I)Ljava/nio/ByteBuffer;

    .line 200
    .line 201
    .line 202
    const v12, 0x20746d66

    .line 203
    .line 204
    .line 205
    invoke-virtual {v11, v12}, Ljava/nio/ByteBuffer;->putInt(I)Ljava/nio/ByteBuffer;

    .line 206
    .line 207
    .line 208
    invoke-virtual {v11, v10}, Ljava/nio/ByteBuffer;->putInt(I)Ljava/nio/ByteBuffer;

    .line 209
    .line 210
    .line 211
    invoke-virtual {v11, v3}, Ljava/nio/ByteBuffer;->putShort(S)Ljava/nio/ByteBuffer;

    .line 212
    .line 213
    .line 214
    invoke-virtual {v11, v3}, Ljava/nio/ByteBuffer;->putShort(S)Ljava/nio/ByteBuffer;

    .line 215
    .line 216
    .line 217
    invoke-virtual {v11, v4}, Ljava/nio/ByteBuffer;->putInt(I)Ljava/nio/ByteBuffer;

    .line 218
    .line 219
    .line 220
    div-int/lit8 v10, v6, 0x8

    .line 221
    .line 222
    mul-int v4, v4, v10

    .line 223
    .line 224
    invoke-virtual {v11, v4}, Ljava/nio/ByteBuffer;->putInt(I)Ljava/nio/ByteBuffer;

    .line 225
    .line 226
    .line 227
    int-to-short v4, v10

    .line 228
    invoke-virtual {v11, v4}, Ljava/nio/ByteBuffer;->putShort(S)Ljava/nio/ByteBuffer;

    .line 229
    .line 230
    .line 231
    int-to-short v4, v6

    .line 232
    invoke-virtual {v11, v4}, Ljava/nio/ByteBuffer;->putShort(S)Ljava/nio/ByteBuffer;

    .line 233
    .line 234
    .line 235
    const v4, 0x61746164

    .line 236
    .line 237
    .line 238
    invoke-virtual {v11, v4}, Ljava/nio/ByteBuffer;->putInt(I)Ljava/nio/ByteBuffer;

    .line 239
    .line 240
    .line 241
    invoke-virtual {v11, v9}, Ljava/nio/ByteBuffer;->putInt(I)Ljava/nio/ByteBuffer;

    .line 242
    .line 243
    .line 244
    iget-object v4, v2, Lf/y52;->VN0:Ljava/nio/ByteBuffer;

    .line 245
    .line 246
    if-eqz v5, :cond_148

    .line 247
    .line 248
    if-eq v5, v3, :cond_134

    .line 249
    .line 250
    if-eq v5, v7, :cond_fc

    .line 251
    .line 252
    goto :goto_15e

    .line 253
    :cond_fc
    iget v5, v2, Lf/y52;->rt:I

    .line 254
    .line 255
    int-to-short v6, v5

    .line 256
    invoke-virtual {v11, v6}, Ljava/nio/ByteBuffer;->putShort(S)Ljava/nio/ByteBuffer;

    .line 257
    .line 258
    .line 259
    invoke-virtual {v4, v8}, Ljava/nio/ByteBuffer;->position(I)Ljava/nio/Buffer;

    .line 260
    .line 261
    .line 262
    invoke-virtual {v4}, Ljava/nio/Buffer;->limit()I

    .line 263
    .line 264
    .line 265
    move-result v6

    .line 266
    new-instance v7, Lf/di;

    .line 267
    .line 268
    const/4 v9, 0x4

    .line 269
    invoke-direct {v7, v9}, Lf/di;-><init>(I)V

    .line 270
    .line 271
    .line 272
    iput v5, v7, Lf/di;->Sm1:I

    .line 273
    .line 274
    iget v2, v2, Lf/y52;->YD1:I

    .line 275
    .line 276
    iput v2, v7, Lf/di;->fE0:I

    .line 277
    .line 278
    :goto_115
    if-ge v8, v6, :cond_15e

    .line 279
    .line 280
    invoke-virtual {v4}, Ljava/nio/ByteBuffer;->get()B

    .line 281
    .line 282
    .line 283
    move-result v2

    .line 284
    add-int/lit8 v8, v8, 0x1

    .line 285
    .line 286
    invoke-static {v2, v7}, Lf/y52;->Rq(BLf/di;)V

    .line 287
    .line 288
    .line 289
    iget v5, v7, Lf/di;->Sm1:I

    .line 290
    .line 291
    int-to-short v5, v5

    .line 292
    invoke-virtual {v11, v5}, Ljava/nio/ByteBuffer;->putShort(S)Ljava/nio/ByteBuffer;

    .line 293
    .line 294
    .line 295
    and-int/lit16 v2, v2, 0xf0

    .line 296
    .line 297
    shr-int/2addr v2, v9

    .line 298
    int-to-byte v2, v2

    .line 299
    invoke-static {v2, v7}, Lf/y52;->Rq(BLf/di;)V

    .line 300
    .line 301
    .line 302
    iget v2, v7, Lf/di;->Sm1:I

    .line 303
    .line 304
    int-to-short v2, v2

    .line 305
    invoke-virtual {v11, v2}, Ljava/nio/ByteBuffer;->putShort(S)Ljava/nio/ByteBuffer;

    .line 306
    .line 307
    .line 308
    goto :goto_115

    .line 309
    :cond_134
    invoke-virtual {v4, v8}, Ljava/nio/ByteBuffer;->position(I)Ljava/nio/Buffer;

    .line 310
    .line 311
    .line 312
    :goto_137
    invoke-virtual {v4}, Ljava/nio/Buffer;->limit()I

    .line 313
    .line 314
    .line 315
    move-result v2

    .line 316
    div-int/2addr v2, v7

    .line 317
    if-ge v8, v2, :cond_15e

    .line 318
    .line 319
    invoke-virtual {v4}, Ljava/nio/ByteBuffer;->getShort()S

    .line 320
    .line 321
    .line 322
    move-result v2

    .line 323
    invoke-virtual {v11, v2}, Ljava/nio/ByteBuffer;->putShort(S)Ljava/nio/ByteBuffer;

    .line 324
    .line 325
    .line 326
    add-int/lit8 v8, v8, 0x1

    .line 327
    .line 328
    goto :goto_137

    .line 329
    :cond_148
    invoke-virtual {v4, v8}, Ljava/nio/ByteBuffer;->position(I)Ljava/nio/Buffer;

    .line 330
    .line 331
    .line 332
    :goto_14b
    invoke-virtual {v4}, Ljava/nio/Buffer;->limit()I

    .line 333
    .line 334
    .line 335
    move-result v2

    .line 336
    if-ge v8, v2, :cond_15e

    .line 337
    .line 338
    invoke-virtual {v4}, Ljava/nio/ByteBuffer;->get()B

    .line 339
    .line 340
    .line 341
    move-result v2

    .line 342
    shl-int/lit8 v2, v2, 0x8

    .line 343
    .line 344
    int-to-short v2, v2

    .line 345
    invoke-virtual {v11, v2}, Ljava/nio/ByteBuffer;->putShort(S)Ljava/nio/ByteBuffer;

    .line 346
    .line 347
    .line 348
    add-int/lit8 v8, v8, 0x1

    .line 349
    .line 350
    goto :goto_14b

    .line 351
    :cond_15e
    :goto_15e
    invoke-virtual {v11}, Ljava/nio/ByteBuffer;->array()[B

    .line 352
    .line 353
    .line 354
    move-result-object v9

    .line 355
    :goto_162
    if-eqz v9, :cond_e

    .line 356
    .line 357
    array-length v2, v9

    .line 358
    if-ge v2, v3, :cond_169

    .line 359
    .line 360
    goto/16 :goto_e

    .line 361
    .line 362
    :cond_169
    new-instance v2, Ljava/util/zip/ZipEntry;

    .line 363
    .line 364
    new-instance v3, Ljava/lang/StringBuilder;

    .line 365
    .line 366
    const-string v4, "cries/"

    .line 367
    .line 368
    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 369
    .line 370
    .line 371
    iget-short v1, v1, Lf/zp3;->Kj1:S

    .line 372
    .line 373
    const-string v4, ".wav"

    .line 374
    .line 375
    invoke-static {v3, v4, v1}, Lf/jp3;->gf(Ljava/lang/StringBuilder;Ljava/lang/String;I)Ljava/lang/String;

    .line 376
    .line 377
    .line 378
    move-result-object v1

    .line 379
    invoke-direct {v2, v1}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 380
    .line 381
    .line 382
    invoke-virtual {p0, v2}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 383
    .line 384
    .line 385
    invoke-virtual {p0, v9}, Ljava/io/OutputStream;->write([B)V

    .line 386
    .line 387
    .line 388
    invoke-virtual {p0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 389
    .line 390
    .line 391
    goto/16 :goto_e

    .line 392
    .line 393
    :cond_188
    return-void
.end method

.method public static hc1(Lf/n15;)V
    .registers 9

    .line 1
    new-instance v0, Lf/y03;

    .line 2
    .line 3
    sget-object v1, Lf/om3$w38;->Sn1:Lf/om3$w38;

    .line 4
    .line 5
    invoke-direct {v0, v1}, Lf/y03;-><init>(Lf/om3$w38;)V

    .line 6
    .line 7
    .line 8
    new-instance v2, Ljava/io/StringWriter;

    .line 9
    .line 10
    invoke-direct {v2}, Ljava/io/StringWriter;-><init>()V

    .line 11
    .line 12
    .line 13
    new-instance v3, Lf/om3;

    .line 14
    .line 15
    invoke-direct {v3, v2}, Lf/om3;-><init>(Ljava/io/Writer;)V

    .line 16
    .line 17
    .line 18
    iput-object v1, v0, Lf/y03;->g90:Lf/om3$w38;

    .line 19
    .line 20
    invoke-virtual {v0, v3}, Lf/y03;->Sd(Ljava/io/Writer;)V

    .line 21
    .line 22
    .line 23
    invoke-virtual {v0}, Lf/y03;->Ou0()V

    .line 24
    .line 25
    .line 26
    new-instance v1, Ljava/util/ArrayList;

    .line 27
    .line 28
    invoke-static {}, Lf/k92;->dh0()Lf/k92;

    .line 29
    .line 30
    .line 31
    move-result-object v2

    .line 32
    invoke-virtual {v2}, Lf/k92;->pa0()Lf/u48;

    .line 33
    .line 34
    .line 35
    move-result-object v2

    .line 36
    invoke-direct {v1, v2}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V

    .line 37
    .line 38
    .line 39
    new-instance v2, Lf/l43;

    .line 40
    .line 41
    const/16 v4, 0x13

    .line 42
    .line 43
    invoke-direct {v2, v4}, Lf/l43;-><init>(I)V

    .line 44
    .line 45
    .line 46
    invoke-static {v2}, Lj$/util/Comparator$-CC;->comparingInt(Ljava/util/function/ToIntFunction;)Ljava/util/Comparator;

    .line 47
    .line 48
    .line 49
    move-result-object v2

    .line 50
    invoke-static {v1, v2}, Ljava/util/Collections;->sort(Ljava/util/List;Ljava/util/Comparator;)V

    .line 51
    .line 52
    .line 53
    invoke-virtual {v1}, Ljava/util/ArrayList;->size()I

    .line 54
    .line 55
    .line 56
    move-result v2

    .line 57
    const/4 v4, 0x0

    .line 58
    :cond_39
    :goto_39
    if-ge v4, v2, :cond_c8

    .line 59
    .line 60
    invoke-virtual {v1, v4}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 61
    .line 62
    .line 63
    move-result-object v5

    .line 64
    add-int/lit8 v4, v4, 0x1

    .line 65
    .line 66
    check-cast v5, Lf/hu6;

    .line 67
    .line 68
    iget-short v6, v5, Lf/hu6;->m21:S

    .line 69
    .line 70
    const/4 v7, 0x1

    .line 71
    if-lt v6, v7, :cond_39

    .line 72
    .line 73
    invoke-virtual {v5}, Lf/hu6;->fz0()Z

    .line 74
    .line 75
    .line 76
    move-result v6

    .line 77
    if-eqz v6, :cond_4f

    .line 78
    .line 79
    goto :goto_39

    .line 80
    :cond_4f
    invoke-virtual {v0}, Lf/y03;->mJ0()V

    .line 81
    .line 82
    .line 83
    iget-short v6, v5, Lf/hu6;->m21:S

    .line 84
    .line 85
    invoke-static {v6}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 86
    .line 87
    .line 88
    move-result-object v6

    .line 89
    const-string v7, "id"

    .line 90
    .line 91
    invoke-virtual {v0, v6, v7}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 92
    .line 93
    .line 94
    iget v6, v5, Lf/hu6;->bl1:I

    .line 95
    .line 96
    invoke-static {v6}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 97
    .line 98
    .line 99
    move-result-object v6

    .line 100
    const-string v7, "name"

    .line 101
    .line 102
    invoke-virtual {v0, v6, v7}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 103
    .line 104
    .line 105
    iget-object v6, v5, Lf/hu6;->iB0:Lf/dd9;

    .line 106
    .line 107
    invoke-virtual {v6}, Lf/dd9;->toString()Ljava/lang/String;

    .line 108
    .line 109
    .line 110
    move-result-object v6

    .line 111
    const-string v7, "skill_damage_type"

    .line 112
    .line 113
    invoke-virtual {v0, v6, v7}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 114
    .line 115
    .line 116
    iget-short v6, v5, Lf/hu6;->r9:S

    .line 117
    .line 118
    invoke-static {v6}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 119
    .line 120
    .line 121
    move-result-object v6

    .line 122
    const-string v7, "base_power"

    .line 123
    .line 124
    invoke-virtual {v0, v6, v7}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 125
    .line 126
    .line 127
    iget-byte v6, v5, Lf/hu6;->dv1:B

    .line 128
    .line 129
    invoke-static {v6}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 130
    .line 131
    .line 132
    move-result-object v6

    .line 133
    const-string v7, "base_accuracy"

    .line 134
    .line 135
    invoke-virtual {v0, v6, v7}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 136
    .line 137
    .line 138
    iget-byte v6, v5, Lf/hu6;->g41:B

    .line 139
    .line 140
    invoke-static {v6}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 141
    .line 142
    .line 143
    move-result-object v6

    .line 144
    const-string v7, "base_pp"

    .line 145
    .line 146
    invoke-virtual {v0, v6, v7}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 147
    .line 148
    .line 149
    iget-byte v6, v5, Lf/hu6;->xx1:B

    .line 150
    .line 151
    invoke-static {v6}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 152
    .line 153
    .line 154
    move-result-object v6

    .line 155
    const-string v7, "priority"

    .line 156
    .line 157
    invoke-virtual {v0, v6, v7}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 158
    .line 159
    .line 160
    const/4 v6, 0x0

    .line 161
    invoke-virtual {v5, v6, v6}, Lf/hu6;->Nw1(Lf/dl6;Lf/ko2;)Lf/eb6;

    .line 162
    .line 163
    .line 164
    move-result-object v6

    .line 165
    invoke-virtual {v6}, Ljava/lang/Object;->toString()Ljava/lang/String;

    .line 166
    .line 167
    .line 168
    move-result-object v6

    .line 169
    const-string v7, "type"

    .line 170
    .line 171
    invoke-virtual {v0, v6, v7}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 172
    .line 173
    .line 174
    iget-byte v6, v5, Lf/hu6;->Bm:B

    .line 175
    .line 176
    invoke-static {v6}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 177
    .line 178
    .line 179
    move-result-object v6

    .line 180
    const-string v7, "target_type"

    .line 181
    .line 182
    invoke-virtual {v0, v6, v7}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 183
    .line 184
    .line 185
    iget-boolean v5, v5, Lf/hu6;->BM0:Z

    .line 186
    .line 187
    invoke-static {v5}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    .line 188
    .line 189
    .line 190
    move-result-object v5

    .line 191
    const-string v6, "true_damage"

    .line 192
    .line 193
    invoke-virtual {v0, v5, v6}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 194
    .line 195
    .line 196
    invoke-virtual {v0}, Lf/y03;->B7()V

    .line 197
    .line 198
    .line 199
    goto/16 :goto_39

    .line 200
    .line 201
    :cond_c8
    invoke-virtual {v0}, Lf/y03;->DE()V

    .line 202
    .line 203
    .line 204
    iget-object v1, v0, Lf/y03;->O30:Lf/om3;

    .line 205
    .line 206
    iget-object v1, v1, Lf/om3;->Zj1:Ljava/io/Writer;

    .line 207
    .line 208
    invoke-virtual {v1}, Ljava/lang/Object;->toString()Ljava/lang/String;

    .line 209
    .line 210
    .line 211
    move-result-object v1

    .line 212
    invoke-virtual {v0, v1}, Lf/y03;->PX(Ljava/lang/String;)Ljava/lang/String;

    .line 213
    .line 214
    .line 215
    move-result-object v0

    .line 216
    new-instance v1, Ljava/util/zip/ZipEntry;

    .line 217
    .line 218
    const-string v2, "info/skills.json"

    .line 219
    .line 220
    invoke-direct {v1, v2}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 221
    .line 222
    .line 223
    invoke-virtual {p0, v1}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 224
    .line 225
    .line 226
    sget-object v1, Ljava/nio/charset/StandardCharsets;->UTF_8:Ljava/nio/charset/Charset;

    .line 227
    .line 228
    invoke-virtual {v0, v1}, Ljava/lang/String;->getBytes(Ljava/nio/charset/Charset;)[B

    .line 229
    .line 230
    .line 231
    move-result-object v0

    .line 232
    invoke-virtual {p0, v0}, Ljava/io/OutputStream;->write([B)V

    .line 233
    .line 234
    .line 235
    invoke-virtual {p0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 236
    .line 237
    .line 238
    invoke-virtual {v3}, Lf/om3;->close()V

    .line 239
    .line 240
    .line 241
    return-void
.end method

.method public static qt(Lf/rd0;Z)Lorg/json/JSONObject;
    .registers 20

    .line 1
    move-object/from16 v0, p0

    .line 2
    .line 3
    new-instance v1, Lorg/json/JSONObject;

    .line 4
    .line 5
    invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V

    .line 6
    .line 7
    .line 8
    if-nez p1, :cond_12

    .line 9
    .line 10
    iget-object v2, v0, Lf/rd0;->TG0:Lf/dl6;

    .line 11
    .line 12
    iget-short v2, v2, Lf/dl6;->AN0:S

    .line 13
    .line 14
    const-string v3, "slot"

    .line 15
    .line 16
    invoke-virtual {v1, v3, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 17
    .line 18
    .line 19
    :cond_12
    iget-object v2, v0, Lf/rd0;->cz0:Lf/zp3;

    .line 20
    .line 21
    iget-object v3, v0, Lf/rd0;->TG0:Lf/dl6;

    .line 22
    .line 23
    iget-object v2, v2, Lf/zp3;->Bu:Lf/ll2;

    .line 24
    .line 25
    iget v2, v2, Lf/ll2;->Yz:I

    .line 26
    .line 27
    const-string v4, "tier_string_id"

    .line 28
    .line 29
    invoke-virtual {v1, v4, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 30
    .line 31
    .line 32
    iget-object v2, v0, Lf/rd0;->cz0:Lf/zp3;

    .line 33
    .line 34
    iget-object v2, v2, Lf/zp3;->Bu:Lf/ll2;

    .line 35
    .line 36
    iget v2, v2, Lf/ll2;->Yz:I

    .line 37
    .line 38
    invoke-static {v2}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 39
    .line 40
    .line 41
    move-result-object v2

    .line 42
    const-string v4, "tier_name"

    .line 43
    .line 44
    invoke-virtual {v1, v4, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 45
    .line 46
    .line 47
    const-string v2, "ot"

    .line 48
    .line 49
    invoke-virtual {v3}, Lf/dl6;->QP()Ljava/lang/String;

    .line 50
    .line 51
    .line 52
    move-result-object v4

    .line 53
    invoke-virtual {v1, v2, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 54
    .line 55
    .line 56
    const-string v2, "name"

    .line 57
    .line 58
    invoke-virtual {v0}, Lf/rd0;->Bl()Ljava/lang/String;

    .line 59
    .line 60
    .line 61
    move-result-object v4

    .line 62
    invoke-virtual {v1, v2, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 63
    .line 64
    .line 65
    const-string v2, "species_name"

    .line 66
    .line 67
    invoke-virtual {v0}, Lf/rd0;->Bn()Ljava/lang/String;

    .line 68
    .line 69
    .line 70
    move-result-object v4

    .line 71
    invoke-virtual {v1, v2, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 72
    .line 73
    .line 74
    const-string v2, "species_id"

    .line 75
    .line 76
    iget-short v4, v3, Lf/dl6;->RB:S

    .line 77
    .line 78
    invoke-virtual {v1, v2, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 79
    .line 80
    .line 81
    const-string v2, "form_id"

    .line 82
    .line 83
    iget-byte v4, v3, Lf/dl6;->rm0:B

    .line 84
    .line 85
    invoke-virtual {v1, v2, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 86
    .line 87
    .line 88
    invoke-virtual {v3}, Lf/dl6;->bk1()Z

    .line 89
    .line 90
    .line 91
    move-result v2

    .line 92
    if-eqz v2, :cond_5e

    .line 93
    .line 94
    return-object v1

    .line 95
    :cond_5e
    const-string v2, "gender"

    .line 96
    .line 97
    invoke-virtual {v0}, Lf/rd0;->w91()B

    .line 98
    .line 99
    .line 100
    move-result v4

    .line 101
    invoke-virtual {v1, v2, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 102
    .line 103
    .line 104
    iget-object v2, v3, Lf/dl6;->u7:Lf/y17;

    .line 105
    .line 106
    iget-byte v2, v2, Lf/y17;->Or0:B

    .line 107
    .line 108
    const v4, 0x2bf20

    .line 109
    .line 110
    .line 111
    add-int/2addr v2, v4

    .line 112
    const-string v4, "nature_string_id"

    .line 113
    .line 114
    invoke-virtual {v1, v4, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 115
    .line 116
    .line 117
    iget-object v2, v3, Lf/dl6;->u7:Lf/y17;

    .line 118
    .line 119
    invoke-virtual {v2}, Lf/y17;->rB0()Ljava/lang/String;

    .line 120
    .line 121
    .line 122
    move-result-object v2

    .line 123
    const-string v4, "nature_name"

    .line 124
    .line 125
    invoke-virtual {v1, v4, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 126
    .line 127
    .line 128
    const-string v2, "ability_id"

    .line 129
    .line 130
    invoke-virtual {v0}, Lf/rd0;->nX()S

    .line 131
    .line 132
    .line 133
    move-result v4

    .line 134
    invoke-virtual {v1, v2, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 135
    .line 136
    .line 137
    const v2, 0x33450

    .line 138
    .line 139
    .line 140
    invoke-virtual {v0}, Lf/rd0;->nX()S

    .line 141
    .line 142
    .line 143
    move-result v0

    .line 144
    add-int/2addr v0, v2

    .line 145
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 146
    .line 147
    .line 148
    move-result-object v0

    .line 149
    const-string v2, "ability_name"

    .line 150
    .line 151
    invoke-virtual {v1, v2, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 152
    .line 153
    .line 154
    const-string v0, "level"

    .line 155
    .line 156
    iget-byte v2, v3, Lf/dl6;->Np0:B

    .line 157
    .line 158
    invoke-virtual {v1, v0, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 159
    .line 160
    .line 161
    const-string v0, "ivs"

    .line 162
    .line 163
    invoke-static {v3}, Lf/tq;->PA1(Lf/dl6;)Ljava/lang/String;

    .line 164
    .line 165
    .line 166
    move-result-object v2

    .line 167
    invoke-virtual {v1, v0, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 168
    .line 169
    .line 170
    const-string v0, "evs"

    .line 171
    .line 172
    invoke-static {v3}, Lf/tq;->Sz(Lf/dl6;)Ljava/lang/String;

    .line 173
    .line 174
    .line 175
    move-result-object v2

    .line 176
    invoke-virtual {v1, v0, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 177
    .line 178
    .line 179
    new-instance v0, Lorg/json/JSONArray;

    .line 180
    .line 181
    invoke-direct {v0}, Lorg/json/JSONArray;-><init>()V

    .line 182
    .line 183
    .line 184
    const/4 v2, 0x0

    .line 185
    const/4 v4, 0x0

    .line 186
    :goto_b9
    const-string v5, "move_name"

    .line 187
    .line 188
    const-string v6, "move_id"

    .line 189
    .line 190
    const/4 v7, 0x3

    .line 191
    const/4 v8, 0x4

    .line 192
    if-ge v4, v8, :cond_fc

    .line 193
    .line 194
    iget-object v8, v3, Lf/dl6;->Oz:[S

    .line 195
    .line 196
    aget-short v8, v8, v4

    .line 197
    .line 198
    if-nez v8, :cond_c8

    .line 199
    .line 200
    goto :goto_f9

    .line 201
    :cond_c8
    invoke-static {}, Lf/k92;->dh0()Lf/k92;

    .line 202
    .line 203
    .line 204
    move-result-object v9

    .line 205
    invoke-virtual {v9, v8}, Lf/k92;->BW1(S)Lf/hu6;

    .line 206
    .line 207
    .line 208
    move-result-object v8

    .line 209
    new-instance v9, Lorg/json/JSONObject;

    .line 210
    .line 211
    invoke-direct {v9}, Lorg/json/JSONObject;-><init>()V

    .line 212
    .line 213
    .line 214
    iget-short v10, v8, Lf/hu6;->m21:S

    .line 215
    .line 216
    invoke-virtual {v9, v6, v10}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 217
    .line 218
    .line 219
    iget v6, v8, Lf/hu6;->bl1:I

    .line 220
    .line 221
    invoke-static {v6}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 222
    .line 223
    .line 224
    move-result-object v6

    .line 225
    invoke-virtual {v9, v5, v6}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 226
    .line 227
    .line 228
    if-ltz v4, :cond_f0

    .line 229
    .line 230
    if-le v4, v7, :cond_e8

    .line 231
    .line 232
    goto :goto_f0

    .line 233
    :cond_e8
    iget-byte v5, v3, Lf/dl6;->throws:B

    .line 234
    .line 235
    mul-int/lit8 v6, v4, 0x2

    .line 236
    .line 237
    shr-int/2addr v5, v6

    .line 238
    and-int/2addr v5, v7

    .line 239
    int-to-byte v5, v5

    .line 240
    goto :goto_f1

    .line 241
    :cond_f0
    :goto_f0
    const/4 v5, 0x0

    .line 242
    :goto_f1
    const-string v6, "pp_bonus"

    .line 243
    .line 244
    invoke-virtual {v9, v6, v5}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 245
    .line 246
    .line 247
    invoke-virtual {v0, v9}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    .line 248
    .line 249
    .line 250
    :goto_f9
    add-int/lit8 v4, v4, 0x1

    .line 251
    .line 252
    goto :goto_b9

    .line 253
    :cond_fc
    const-string v4, "moves"

    .line 254
    .line 255
    invoke-virtual {v1, v4, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 256
    .line 257
    .line 258
    iget-object v0, v3, Lf/dl6;->nE0:[S

    .line 259
    .line 260
    new-instance v4, Lorg/json/JSONArray;

    .line 261
    .line 262
    invoke-direct {v4}, Lorg/json/JSONArray;-><init>()V

    .line 263
    .line 264
    .line 265
    array-length v9, v0

    .line 266
    const/4 v10, 0x0

    .line 267
    :goto_10a
    if-ge v10, v9, :cond_132

    .line 268
    .line 269
    aget-short v11, v0, v10

    .line 270
    .line 271
    if-nez v11, :cond_111

    .line 272
    .line 273
    goto :goto_12f

    .line 274
    :cond_111
    invoke-static {}, Lf/k92;->dh0()Lf/k92;

    .line 275
    .line 276
    .line 277
    move-result-object v12

    .line 278
    invoke-virtual {v12, v11}, Lf/k92;->BW1(S)Lf/hu6;

    .line 279
    .line 280
    .line 281
    move-result-object v11

    .line 282
    new-instance v12, Lorg/json/JSONObject;

    .line 283
    .line 284
    invoke-direct {v12}, Lorg/json/JSONObject;-><init>()V

    .line 285
    .line 286
    .line 287
    iget-short v13, v11, Lf/hu6;->m21:S

    .line 288
    .line 289
    invoke-virtual {v12, v6, v13}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 290
    .line 291
    .line 292
    iget v11, v11, Lf/hu6;->bl1:I

    .line 293
    .line 294
    invoke-static {v11}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 295
    .line 296
    .line 297
    move-result-object v11

    .line 298
    invoke-virtual {v12, v5, v11}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 299
    .line 300
    .line 301
    invoke-virtual {v4, v12}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    .line 302
    .line 303
    .line 304
    :goto_12f
    add-int/lit8 v10, v10, 0x1

    .line 305
    .line 306
    goto :goto_10a

    .line 307
    :cond_132
    const-string v0, "hatch_moves"

    .line 308
    .line 309
    invoke-virtual {v1, v0, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 310
    .line 311
    .line 312
    invoke-virtual {v3}, Lf/dl6;->QT1()S

    .line 313
    .line 314
    .line 315
    move-result v0

    .line 316
    const-string v4, "held_item_name"

    .line 317
    .line 318
    const-string v5, "held_item_id"

    .line 319
    .line 320
    if-eqz v0, :cond_15a

    .line 321
    .line 322
    sget-object v0, Lf/an8;->LU:Lf/an8;

    .line 323
    .line 324
    invoke-virtual {v3}, Lf/dl6;->QT1()S

    .line 325
    .line 326
    .line 327
    move-result v6

    .line 328
    invoke-virtual {v0, v6}, Lf/an8;->R3(S)Lf/ls0;

    .line 329
    .line 330
    .line 331
    move-result-object v0

    .line 332
    iget-short v6, v0, Lf/ls0;->C4:S

    .line 333
    .line 334
    invoke-virtual {v1, v5, v6}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 335
    .line 336
    .line 337
    iget v0, v0, Lf/ls0;->FU:I

    .line 338
    .line 339
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 340
    .line 341
    .line 342
    move-result-object v0

    .line 343
    :goto_156
    invoke-virtual {v1, v4, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 344
    .line 345
    .line 346
    goto :goto_160

    .line 347
    :cond_15a
    invoke-virtual {v1, v5, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 348
    .line 349
    .line 350
    const-string v0, "-"

    .line 351
    .line 352
    goto :goto_156

    .line 353
    :goto_160
    new-instance v0, Lorg/json/JSONArray;

    .line 354
    .line 355
    invoke-direct {v0}, Lorg/json/JSONArray;-><init>()V

    .line 356
    .line 357
    .line 358
    iget-object v4, v3, Lf/dl6;->M1:[Lf/c89;

    .line 359
    .line 360
    array-length v5, v4

    .line 361
    const/4 v6, 0x0

    .line 362
    :goto_169
    if-ge v6, v5, :cond_18a

    .line 363
    .line 364
    aget-object v9, v4, v6

    .line 365
    .line 366
    new-instance v10, Lorg/json/JSONObject;

    .line 367
    .line 368
    invoke-direct {v10}, Lorg/json/JSONObject;-><init>()V

    .line 369
    .line 370
    .line 371
    const-string v11, "particle_string_id"

    .line 372
    .line 373
    invoke-virtual {v9}, Lf/c89;->qp0()I

    .line 374
    .line 375
    .line 376
    move-result v12

    .line 377
    invoke-virtual {v10, v11, v12}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 378
    .line 379
    .line 380
    const-string v11, "particle_name"

    .line 381
    .line 382
    invoke-virtual {v9}, Lf/c89;->toString()Ljava/lang/String;

    .line 383
    .line 384
    .line 385
    move-result-object v9

    .line 386
    invoke-virtual {v10, v11, v9}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 387
    .line 388
    .line 389
    invoke-virtual {v0, v10}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    .line 390
    .line 391
    .line 392
    add-int/lit8 v6, v6, 0x1

    .line 393
    .line 394
    goto :goto_169

    .line 395
    :cond_18a
    const-string v4, "particles"

    .line 396
    .line 397
    invoke-virtual {v1, v4, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 398
    .line 399
    .line 400
    new-instance v0, Lorg/json/JSONArray;

    .line 401
    .line 402
    invoke-direct {v0}, Lorg/json/JSONArray;-><init>()V

    .line 403
    .line 404
    .line 405
    sget-object v4, Lf/gk6;->CQ:[[I

    .line 406
    .line 407
    array-length v5, v4

    .line 408
    const/4 v6, 0x0

    .line 409
    :goto_198
    const/16 v9, 0xaa

    .line 410
    .line 411
    const-string v10, "ribbon_name"

    .line 412
    .line 413
    const-string v11, "ribbon_id"

    .line 414
    .line 415
    if-ge v6, v5, :cond_1c8

    .line 416
    .line 417
    aget-object v12, v4, v6

    .line 418
    .line 419
    aget v13, v12, v2

    .line 420
    .line 421
    invoke-virtual {v3, v13}, Lf/dl6;->COm7(I)Z

    .line 422
    .line 423
    .line 424
    move-result v13

    .line 425
    if-nez v13, :cond_1ab

    .line 426
    .line 427
    goto :goto_1c5

    .line 428
    :cond_1ab
    new-instance v13, Lorg/json/JSONObject;

    .line 429
    .line 430
    invoke-direct {v13}, Lorg/json/JSONObject;-><init>()V

    .line 431
    .line 432
    .line 433
    aget v14, v12, v2

    .line 434
    .line 435
    add-int/lit8 v14, v14, -0x10

    .line 436
    .line 437
    invoke-virtual {v13, v11, v14}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 438
    .line 439
    .line 440
    sget-object v11, Lf/pn2;->SD:Lf/pn2;

    .line 441
    .line 442
    aget v12, v12, v7

    .line 443
    .line 444
    invoke-static {v11, v9, v12}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 445
    .line 446
    .line 447
    move-result-object v9

    .line 448
    invoke-virtual {v13, v10, v9}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 449
    .line 450
    .line 451
    invoke-virtual {v0, v13}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    .line 452
    .line 453
    .line 454
    :goto_1c5
    add-int/lit8 v6, v6, 0x1

    .line 455
    .line 456
    goto :goto_198

    .line 457
    :cond_1c8
    sget-object v4, Lf/gk6;->Qa:[[[I

    .line 458
    .line 459
    array-length v5, v4

    .line 460
    const/4 v6, 0x0

    .line 461
    :goto_1cc
    const/4 v12, 0x1

    .line 462
    if-ge v6, v5, :cond_21b

    .line 463
    .line 464
    aget-object v13, v4, v6

    .line 465
    .line 466
    array-length v14, v13

    .line 467
    const/4 v15, 0x0

    .line 468
    :goto_1d3
    if-ge v15, v14, :cond_20f

    .line 469
    .line 470
    aget-object v16, v13, v15

    .line 471
    .line 472
    const/16 p0, 0x0

    .line 473
    .line 474
    aget v2, v16, p0

    .line 475
    .line 476
    const/16 p1, 0x4

    .line 477
    .line 478
    aget v8, v16, v12

    .line 479
    .line 480
    invoke-virtual {v3, v2, v8}, Lf/dl6;->Uf1(II)Z

    .line 481
    .line 482
    .line 483
    move-result v2

    .line 484
    if-nez v2, :cond_1e8

    .line 485
    .line 486
    const/16 v17, 0x3

    .line 487
    .line 488
    goto :goto_209

    .line 489
    :cond_1e8
    new-instance v2, Lorg/json/JSONObject;

    .line 490
    .line 491
    invoke-direct {v2}, Lorg/json/JSONObject;-><init>()V

    .line 492
    .line 493
    .line 494
    sget-object v8, Lf/pn2;->SD:Lf/pn2;

    .line 495
    .line 496
    const/16 v17, 0x3

    .line 497
    .line 498
    aget v7, v16, v17

    .line 499
    .line 500
    invoke-static {v8, v9, v7}, Lf/gt0;->WP0(Lf/pn2;II)Ljava/lang/String;

    .line 501
    .line 502
    .line 503
    move-result-object v7

    .line 504
    invoke-virtual {v2, v10, v7}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 505
    .line 506
    .line 507
    aget v7, v16, p0

    .line 508
    .line 509
    mul-int/lit8 v7, v7, 0x4

    .line 510
    .line 511
    aget v8, v16, v12

    .line 512
    .line 513
    add-int/2addr v7, v8

    .line 514
    add-int/lit8 v7, v7, 0x12

    .line 515
    .line 516
    invoke-virtual {v2, v11, v7}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 517
    .line 518
    .line 519
    invoke-virtual {v0, v2}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    .line 520
    .line 521
    .line 522
    :goto_209
    add-int/lit8 v15, v15, 0x1

    .line 523
    .line 524
    const/4 v2, 0x0

    .line 525
    const/4 v7, 0x3

    .line 526
    const/4 v8, 0x4

    .line 527
    goto :goto_1d3

    .line 528
    :cond_20f
    const/16 p0, 0x0

    .line 529
    .line 530
    const/16 p1, 0x4

    .line 531
    .line 532
    const/16 v17, 0x3

    .line 533
    .line 534
    add-int/lit8 v6, v6, 0x1

    .line 535
    .line 536
    const/4 v2, 0x0

    .line 537
    const/4 v7, 0x3

    .line 538
    const/4 v8, 0x4

    .line 539
    goto :goto_1cc

    .line 540
    :cond_21b
    const/16 p0, 0x0

    .line 541
    .line 542
    const/16 p1, 0x4

    .line 543
    .line 544
    const/16 v17, 0x3

    .line 545
    .line 546
    const-string v2, "ribbons"

    .line 547
    .line 548
    invoke-virtual {v1, v2, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 549
    .line 550
    .line 551
    new-instance v0, Lorg/json/JSONObject;

    .line 552
    .line 553
    invoke-direct {v0}, Lorg/json/JSONObject;-><init>()V

    .line 554
    .line 555
    .line 556
    sget-object v2, Lf/te1;->OZ1:[Lf/te1;

    .line 557
    .line 558
    array-length v4, v2

    .line 559
    const/4 v5, 0x0

    .line 560
    :goto_22f
    if-ge v5, v4, :cond_269

    .line 561
    .line 562
    aget-object v6, v2, v5

    .line 563
    .line 564
    invoke-virtual {v3, v6}, Lf/dl6;->uK0(Lf/te1;)S

    .line 565
    .line 566
    .line 567
    move-result v7

    .line 568
    sget-object v8, Lf/tt7;->s01:[I

    .line 569
    .line 570
    iget v6, v6, Lf/te1;->nm:I

    .line 571
    .line 572
    aget v6, v8, v6

    .line 573
    .line 574
    if-eq v6, v12, :cond_25d

    .line 575
    .line 576
    const/4 v8, 0x2

    .line 577
    if-eq v6, v8, :cond_258

    .line 578
    .line 579
    const/4 v8, 0x3

    .line 580
    const/4 v9, 0x4

    .line 581
    if-eq v6, v8, :cond_255

    .line 582
    .line 583
    if-eq v6, v9, :cond_252

    .line 584
    .line 585
    const/4 v10, 0x5

    .line 586
    if-eq v6, v10, :cond_24c

    .line 587
    .line 588
    goto :goto_262

    .line 589
    :cond_24c
    const-string v6, "toughness"

    .line 590
    .line 591
    :goto_24e
    invoke-virtual {v0, v6, v7}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 592
    .line 593
    .line 594
    goto :goto_262

    .line 595
    :cond_252
    const-string v6, "smartness"

    .line 596
    .line 597
    goto :goto_24e

    .line 598
    :cond_255
    const-string v6, "cuteness"

    .line 599
    .line 600
    goto :goto_24e

    .line 601
    :cond_258
    const/4 v8, 0x3

    .line 602
    const/4 v9, 0x4

    .line 603
    const-string v6, "beauty"

    .line 604
    .line 605
    goto :goto_24e

    .line 606
    :cond_25d
    const/4 v8, 0x3

    .line 607
    const/4 v9, 0x4

    .line 608
    const-string v6, "coolness"

    .line 609
    .line 610
    goto :goto_24e

    .line 611
    :goto_262
    add-int/lit8 v5, v5, 0x1

    .line 612
    .line 613
    const/16 p1, 0x4

    .line 614
    .line 615
    const/16 v17, 0x3

    .line 616
    .line 617
    goto :goto_22f

    .line 618
    :cond_269
    const-string v2, "contest_stats"

    .line 619
    .line 620
    invoke-virtual {v1, v2, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 621
    .line 622
    .line 623
    const-string v0, "happiness"

    .line 624
    .line 625
    iget-short v2, v3, Lf/dl6;->P5:S

    .line 626
    .line 627
    invoke-virtual {v1, v0, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 628
    .line 629
    .line 630
    invoke-virtual {v3}, Lf/dl6;->ev()Lf/eb6;

    .line 631
    .line 632
    .line 633
    move-result-object v0

    .line 634
    const v2, 0x38270

    .line 635
    .line 636
    .line 637
    iget-byte v0, v0, Lf/eb6;->qx:B

    .line 638
    .line 639
    add-int/2addr v0, v2

    .line 640
    const-string v2, "applied_gem_type_string_id"

    .line 641
    .line 642
    invoke-virtual {v1, v2, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 643
    .line 644
    .line 645
    invoke-virtual {v3}, Lf/dl6;->ev()Lf/eb6;

    .line 646
    .line 647
    .line 648
    move-result-object v0

    .line 649
    invoke-virtual {v0}, Lf/eb6;->IN0()Ljava/lang/String;

    .line 650
    .line 651
    .line 652
    move-result-object v0

    .line 653
    const-string v2, "applied_gem_type_name"

    .line 654
    .line 655
    invoke-virtual {v1, v2, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 656
    .line 657
    .line 658
    const-string v0, "experience"

    .line 659
    .line 660
    iget v2, v3, Lf/dl6;->M2:I

    .line 661
    .line 662
    invoke-virtual {v1, v0, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 663
    .line 664
    .line 665
    sget-object v0, Lf/an8;->LU:Lf/an8;

    .line 666
    .line 667
    iget-byte v2, v3, Lf/dl6;->Nq0:B

    .line 668
    .line 669
    iget-object v0, v0, Lf/an8;->BD:Lf/k33;

    .line 670
    .line 671
    invoke-virtual {v0, v2}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 672
    .line 673
    .line 674
    move-result-object v0

    .line 675
    check-cast v0, Lf/ls0;

    .line 676
    .line 677
    const-string v2, "current_ball_id"

    .line 678
    .line 679
    iget-short v4, v0, Lf/ls0;->C4:S

    .line 680
    .line 681
    invoke-virtual {v1, v2, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 682
    .line 683
    .line 684
    iget v0, v0, Lf/ls0;->FU:I

    .line 685
    .line 686
    invoke-static {v0}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 687
    .line 688
    .line 689
    move-result-object v0

    .line 690
    const-string v2, "current_ball_name"

    .line 691
    .line 692
    invoke-virtual {v1, v2, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 693
    .line 694
    .line 695
    const-string v0, "hidden_ability"

    .line 696
    .line 697
    invoke-virtual {v3}, Lf/dl6;->Pp0()Z

    .line 698
    .line 699
    .line 700
    move-result v2

    .line 701
    invoke-virtual {v1, v0, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;

    .line 702
    .line 703
    .line 704
    const-string v0, "shiny"

    .line 705
    .line 706
    invoke-virtual {v3}, Lf/dl6;->OB0()Z

    .line 707
    .line 708
    .line 709
    move-result v2

    .line 710
    invoke-virtual {v1, v0, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;

    .line 711
    .line 712
    .line 713
    const-string v0, "secret_shiny"

    .line 714
    .line 715
    invoke-virtual {v3}, Lf/dl6;->mk0()Z

    .line 716
    .line 717
    .line 718
    move-result v2

    .line 719
    invoke-virtual {v1, v0, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;

    .line 720
    .line 721
    .line 722
    const-string v0, "alpha"

    .line 723
    .line 724
    invoke-virtual {v3}, Lf/dl6;->Wm1()Z

    .line 725
    .line 726
    .line 727
    move-result v2

    .line 728
    invoke-virtual {v1, v0, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;

    .line 729
    .line 730
    .line 731
    new-instance v0, Lorg/json/JSONObject;

    .line 732
    .line 733
    invoke-direct {v0}, Lorg/json/JSONObject;-><init>()V

    .line 734
    .line 735
    .line 736
    const-string v2, "region_caught_id"

    .line 737
    .line 738
    iget-byte v4, v3, Lf/dl6;->BC:B

    .line 739
    .line 740
    invoke-virtual {v0, v2, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 741
    .line 742
    .line 743
    const v2, 0x3d090

    .line 744
    .line 745
    .line 746
    iget-byte v4, v3, Lf/dl6;->BC:B

    .line 747
    .line 748
    add-int/2addr v4, v2

    .line 749
    invoke-static {v4}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 750
    .line 751
    .line 752
    move-result-object v2

    .line 753
    const-string v4, "region_caught_name"

    .line 754
    .line 755
    invoke-virtual {v0, v4, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 756
    .line 757
    .line 758
    const-string v2, "location_caught_id"

    .line 759
    .line 760
    iget-byte v4, v3, Lf/dl6;->t9:B

    .line 761
    .line 762
    invoke-virtual {v0, v2, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 763
    .line 764
    .line 765
    sget-object v2, Lf/tq;->dp:Ljava/text/DecimalFormat;

    .line 766
    .line 767
    iget-byte v2, v3, Lf/dl6;->BC:B

    .line 768
    .line 769
    iget-byte v4, v3, Lf/dl6;->t9:B

    .line 770
    .line 771
    iget-byte v5, v3, Lf/dl6;->RW0:B

    .line 772
    .line 773
    invoke-static {v2, v4, v5}, Lf/tq;->rM(BBB)Ljava/lang/String;

    .line 774
    .line 775
    .line 776
    move-result-object v2

    .line 777
    const-string v4, "location_caught_name"

    .line 778
    .line 779
    invoke-virtual {v0, v4, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 780
    .line 781
    .line 782
    iget v2, v3, Lf/dl6;->LV1:I

    .line 783
    .line 784
    int-to-long v4, v2

    .line 785
    const-wide/16 v6, 0x3e8

    .line 786
    .line 787
    mul-long v4, v4, v6

    .line 788
    .line 789
    const-string v2, "date_caught"

    .line 790
    .line 791
    invoke-virtual {v0, v2, v4, v5}, Lorg/json/JSONObject;->put(Ljava/lang/String;J)Lorg/json/JSONObject;

    .line 792
    .line 793
    .line 794
    const-string v2, "level_caught"

    .line 795
    .line 796
    iget-byte v4, v3, Lf/dl6;->RW0:B

    .line 797
    .line 798
    invoke-virtual {v0, v2, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 799
    .line 800
    .line 801
    const-string v2, "shiny_encounter_caught"

    .line 802
    .line 803
    iget v4, v3, Lf/dl6;->Xt0:I

    .line 804
    .line 805
    invoke-virtual {v0, v2, v4}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    .line 806
    .line 807
    .line 808
    iget-byte v2, v3, Lf/dl6;->RW0:B

    .line 809
    .line 810
    const/4 v4, -0x1

    .line 811
    if-ne v2, v4, :cond_32e

    .line 812
    .line 813
    const/4 v2, 0x1

    .line 814
    goto :goto_32f

    .line 815
    :cond_32e
    const/4 v2, 0x0

    .line 816
    :goto_32f
    const-string v4, "is_hatched"

    .line 817
    .line 818
    invoke-virtual {v0, v4, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;

    .line 819
    .line 820
    .line 821
    iget-short v2, v3, Lf/dl6;->D20:S

    .line 822
    .line 823
    and-int/lit8 v2, v2, 0x40

    .line 824
    .line 825
    if-eqz v2, :cond_33c

    .line 826
    .line 827
    const/4 v2, 0x1

    .line 828
    goto :goto_33d

    .line 829
    :cond_33c
    const/4 v2, 0x0

    .line 830
    :goto_33d
    const-string v4, "raid_encounter"

    .line 831
    .line 832
    invoke-virtual {v0, v4, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;

    .line 833
    .line 834
    .line 835
    const-string v2, "fateful_encounter"

    .line 836
    .line 837
    invoke-virtual {v3}, Lf/dl6;->Mh0()Z

    .line 838
    .line 839
    .line 840
    move-result v3

    .line 841
    invoke-virtual {v0, v2, v3}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;

    .line 842
    .line 843
    .line 844
    const-string v2, "capture_data"

    .line 845
    .line 846
    invoke-virtual {v1, v2, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    .line 847
    .line 848
    .line 849
    return-object v1
.end method

.method public static tK1(Lf/n15;)V
    .registers 22

    .line 1
    move-object/from16 v0, p0

    .line 2
    .line 3
    new-instance v1, Lf/y03;

    .line 4
    .line 5
    sget-object v2, Lf/om3$w38;->Sn1:Lf/om3$w38;

    .line 6
    .line 7
    invoke-direct {v1, v2}, Lf/y03;-><init>(Lf/om3$w38;)V

    .line 8
    .line 9
    .line 10
    new-instance v3, Ljava/io/StringWriter;

    .line 11
    .line 12
    invoke-direct {v3}, Ljava/io/StringWriter;-><init>()V

    .line 13
    .line 14
    .line 15
    new-instance v4, Lf/om3;

    .line 16
    .line 17
    invoke-direct {v4, v3}, Lf/om3;-><init>(Ljava/io/Writer;)V

    .line 18
    .line 19
    .line 20
    iput-object v2, v1, Lf/y03;->g90:Lf/om3$w38;

    .line 21
    .line 22
    invoke-virtual {v1, v4}, Lf/y03;->Sd(Ljava/io/Writer;)V

    .line 23
    .line 24
    .line 25
    invoke-virtual {v1}, Lf/y03;->Ou0()V

    .line 26
    .line 27
    .line 28
    new-instance v2, Ljava/util/ArrayList;

    .line 29
    .line 30
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 31
    .line 32
    .line 33
    move-result-object v3

    .line 34
    iget-object v3, v3, Lf/y91;->Uv:Ljava/util/HashMap;

    .line 35
    .line 36
    invoke-virtual {v3}, Ljava/util/HashMap;->values()Ljava/util/Collection;

    .line 37
    .line 38
    .line 39
    move-result-object v3

    .line 40
    invoke-direct {v2, v3}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V

    .line 41
    .line 42
    .line 43
    new-instance v3, Lf/l43;

    .line 44
    .line 45
    const/4 v5, 0x7

    .line 46
    invoke-direct {v3, v5}, Lf/l43;-><init>(I)V

    .line 47
    .line 48
    .line 49
    invoke-static {v3}, Lj$/util/Comparator$-CC;->comparingInt(Ljava/util/function/ToIntFunction;)Ljava/util/Comparator;

    .line 50
    .line 51
    .line 52
    move-result-object v3

    .line 53
    invoke-static {v2, v3}, Ljava/util/Collections;->sort(Ljava/util/List;Ljava/util/Comparator;)V

    .line 54
    .line 55
    .line 56
    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I

    .line 57
    .line 58
    .line 59
    move-result v3

    .line 60
    const/4 v5, 0x0

    .line 61
    const/4 v6, 0x0

    .line 62
    :cond_3d
    :goto_3d
    if-ge v6, v3, :cond_482

    .line 63
    .line 64
    invoke-virtual {v2, v6}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 65
    .line 66
    .line 67
    move-result-object v7

    .line 68
    add-int/lit8 v6, v6, 0x1

    .line 69
    .line 70
    check-cast v7, Lf/zp3;

    .line 71
    .line 72
    iget-short v8, v7, Lf/zp3;->Kj1:S

    .line 73
    .line 74
    const/4 v9, 0x1

    .line 75
    if-lt v8, v9, :cond_3d

    .line 76
    .line 77
    iget-object v10, v7, Lf/zp3;->wd1:Lf/wq3;

    .line 78
    .line 79
    sget-object v11, Lf/wq3;->UK1:Lf/wq3;

    .line 80
    .line 81
    if-ne v10, v11, :cond_53

    .line 82
    .line 83
    goto :goto_3d

    .line 84
    :cond_53
    invoke-virtual {v1}, Lf/y03;->mJ0()V

    .line 85
    .line 86
    .line 87
    invoke-static {v8}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 88
    .line 89
    .line 90
    move-result-object v10

    .line 91
    const-string v11, "id"

    .line 92
    .line 93
    invoke-virtual {v1, v10, v11}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 94
    .line 95
    .line 96
    invoke-virtual {v7, v5}, Lf/zp3;->xt(Z)Ljava/lang/String;

    .line 97
    .line 98
    .line 99
    move-result-object v10

    .line 100
    const-string v12, "name"

    .line 101
    .line 102
    invoke-virtual {v1, v10, v12}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 103
    .line 104
    .line 105
    iget-object v10, v7, Lf/zp3;->R:Lf/o9;

    .line 106
    .line 107
    iget-byte v10, v10, Lf/o9;->rf:B

    .line 108
    .line 109
    invoke-static {v10}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 110
    .line 111
    .line 112
    move-result-object v10

    .line 113
    const-string v13, "exp_type"

    .line 114
    .line 115
    invoke-virtual {v1, v10, v13}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 116
    .line 117
    .line 118
    iget-object v10, v7, Lf/zp3;->R:Lf/o9;

    .line 119
    .line 120
    invoke-virtual {v10}, Lf/o9;->toString()Ljava/lang/String;

    .line 121
    .line 122
    .line 123
    move-result-object v10

    .line 124
    const-string v13, "exp_type_name"

    .line 125
    .line 126
    invoke-virtual {v1, v10, v13}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 127
    .line 128
    .line 129
    iget v10, v7, Lf/zp3;->bj:I

    .line 130
    .line 131
    invoke-static {v10}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 132
    .line 133
    .line 134
    move-result-object v10

    .line 135
    const-string v13, "catch_rate"

    .line 136
    .line 137
    invoke-virtual {v1, v10, v13}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 138
    .line 139
    .line 140
    iget-boolean v10, v7, Lf/zp3;->Nm1:Z

    .line 141
    .line 142
    xor-int/2addr v10, v9

    .line 143
    invoke-static {v10}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    .line 144
    .line 145
    .line 146
    move-result-object v10

    .line 147
    const-string v13, "obtainable"

    .line 148
    .line 149
    invoke-virtual {v1, v10, v13}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 150
    .line 151
    .line 152
    iget-short v10, v7, Lf/zp3;->VF0:S

    .line 153
    .line 154
    invoke-static {v10}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 155
    .line 156
    .line 157
    move-result-object v10

    .line 158
    const-string v13, "gender_ratio"

    .line 159
    .line 160
    invoke-virtual {v1, v10, v13}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 161
    .line 162
    .line 163
    iget-short v10, v7, Lf/zp3;->YV1:S

    .line 164
    .line 165
    invoke-static {v10}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 166
    .line 167
    .line 168
    move-result-object v10

    .line 169
    const-string v13, "height"

    .line 170
    .line 171
    invoke-virtual {v1, v10, v13}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 172
    .line 173
    .line 174
    iget-short v10, v7, Lf/zp3;->Rj:S

    .line 175
    .line 176
    invoke-static {v10}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 177
    .line 178
    .line 179
    move-result-object v10

    .line 180
    const-string v13, "weight"

    .line 181
    .line 182
    invoke-virtual {v1, v10, v13}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 183
    .line 184
    .line 185
    const-string v10, "egg_groups"

    .line 186
    .line 187
    invoke-virtual {v1, v10}, Lf/y03;->SJ1(Ljava/lang/String;)V

    .line 188
    .line 189
    .line 190
    iget-object v10, v7, Lf/zp3;->Gj:Lf/lo1;

    .line 191
    .line 192
    iget-object v13, v7, Lf/zp3;->wa:Lf/lo1;

    .line 193
    .line 194
    if-ne v10, v13, :cond_c8

    .line 195
    .line 196
    new-array v13, v9, [Lf/lo1;

    .line 197
    .line 198
    aput-object v10, v13, v5

    .line 199
    .line 200
    goto :goto_d0

    .line 201
    :cond_c8
    const/4 v14, 0x2

    .line 202
    new-array v14, v14, [Lf/lo1;

    .line 203
    .line 204
    aput-object v10, v14, v5

    .line 205
    .line 206
    aput-object v13, v14, v9

    .line 207
    .line 208
    move-object v13, v14

    .line 209
    :goto_d0
    array-length v10, v13

    .line 210
    const/4 v14, 0x0

    .line 211
    :goto_d2
    if-ge v14, v10, :cond_eb

    .line 212
    .line 213
    aget-object v15, v13, v14

    .line 214
    .line 215
    iget-byte v15, v15, Lf/lo1;->XY1:B

    .line 216
    .line 217
    const v16, 0x2c308

    .line 218
    .line 219
    .line 220
    add-int v15, v15, v16

    .line 221
    .line 222
    invoke-static {v15}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 223
    .line 224
    .line 225
    move-result-object v15

    .line 226
    invoke-virtual {v15}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;

    .line 227
    .line 228
    .line 229
    move-result-object v15

    .line 230
    invoke-virtual {v1, v15}, Lf/y03;->dZ0(Ljava/lang/String;)V

    .line 231
    .line 232
    .line 233
    add-int/lit8 v14, v14, 0x1

    .line 234
    .line 235
    goto :goto_d2

    .line 236
    :cond_eb
    invoke-virtual {v1}, Lf/y03;->DE()V

    .line 237
    .line 238
    .line 239
    const-string v10, "abilities"

    .line 240
    .line 241
    invoke-virtual {v1, v10}, Lf/y03;->SJ1(Ljava/lang/String;)V

    .line 242
    .line 243
    .line 244
    const/4 v10, 0x0

    .line 245
    :goto_f4
    const/4 v13, 0x3

    .line 246
    if-ge v10, v13, :cond_11a

    .line 247
    .line 248
    invoke-virtual {v1}, Lf/y03;->mJ0()V

    .line 249
    .line 250
    .line 251
    invoke-virtual {v7, v10}, Lf/zp3;->t00(I)S

    .line 252
    .line 253
    .line 254
    move-result v13

    .line 255
    invoke-static {v13}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 256
    .line 257
    .line 258
    move-result-object v13

    .line 259
    invoke-virtual {v1, v13, v11}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 260
    .line 261
    .line 262
    const v13, 0x33450

    .line 263
    .line 264
    .line 265
    invoke-virtual {v7, v10}, Lf/zp3;->t00(I)S

    .line 266
    .line 267
    .line 268
    move-result v14

    .line 269
    add-int/2addr v14, v13

    .line 270
    invoke-static {v14}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 271
    .line 272
    .line 273
    move-result-object v13

    .line 274
    invoke-virtual {v1, v13, v12}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 275
    .line 276
    .line 277
    invoke-virtual {v1}, Lf/y03;->B7()V

    .line 278
    .line 279
    .line 280
    add-int/lit8 v10, v10, 0x1

    .line 281
    .line 282
    goto :goto_f4

    .line 283
    :cond_11a
    invoke-virtual {v1}, Lf/y03;->DE()V

    .line 284
    .line 285
    .line 286
    invoke-virtual {v7}, Lf/zp3;->nd0()Z

    .line 287
    .line 288
    .line 289
    move-result v10

    .line 290
    if-nez v10, :cond_184

    .line 291
    .line 292
    const-string v10, "forms"

    .line 293
    .line 294
    invoke-virtual {v1, v10}, Lf/y03;->SJ1(Ljava/lang/String;)V

    .line 295
    .line 296
    .line 297
    const/4 v10, 0x0

    .line 298
    :goto_129
    iget-object v13, v7, Lf/zp3;->Pq:Lf/k33;

    .line 299
    .line 300
    iget v13, v13, Lf/ip8;->Mf1:I

    .line 301
    .line 302
    int-to-byte v13, v13

    .line 303
    if-ge v10, v13, :cond_181

    .line 304
    .line 305
    invoke-virtual {v1}, Lf/y03;->mJ0()V

    .line 306
    .line 307
    .line 308
    invoke-virtual {v7, v10}, Lf/zp3;->x1(B)Z

    .line 309
    .line 310
    .line 311
    move-result v13

    .line 312
    if-eqz v13, :cond_146

    .line 313
    .line 314
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 315
    .line 316
    .line 317
    move-result-object v13

    .line 318
    invoke-virtual {v7, v10}, Lf/zp3;->hC1(B)S

    .line 319
    .line 320
    .line 321
    move-result v14

    .line 322
    invoke-virtual {v13, v14}, Lf/y91;->wT0(S)Lf/zp3;

    .line 323
    .line 324
    .line 325
    move-result-object v13

    .line 326
    goto :goto_147

    .line 327
    :cond_146
    move-object v13, v7

    .line 328
    :goto_147
    const-string v14, "form_id"

    .line 329
    .line 330
    invoke-static {v10}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 331
    .line 332
    .line 333
    move-result-object v15

    .line 334
    invoke-virtual {v1, v15, v14}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 335
    .line 336
    .line 337
    iget-short v14, v13, Lf/zp3;->Kj1:S

    .line 338
    .line 339
    invoke-static {v14}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 340
    .line 341
    .line 342
    move-result-object v14

    .line 343
    invoke-virtual {v1, v14, v11}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 344
    .line 345
    .line 346
    invoke-virtual {v13, v9}, Lf/zp3;->xt(Z)Ljava/lang/String;

    .line 347
    .line 348
    .line 349
    move-result-object v14

    .line 350
    invoke-virtual {v1, v14, v12}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 351
    .line 352
    .line 353
    invoke-virtual {v13, v10}, Lf/zp3;->Lt0(B)Z

    .line 354
    .line 355
    .line 356
    move-result v14

    .line 357
    invoke-static {v14}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    .line 358
    .line 359
    .line 360
    move-result-object v14

    .line 361
    const-string v15, "is_costume"

    .line 362
    .line 363
    invoke-virtual {v1, v14, v15}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 364
    .line 365
    .line 366
    invoke-virtual {v13, v10}, Lf/zp3;->iA(B)Z

    .line 367
    .line 368
    .line 369
    move-result v13

    .line 370
    invoke-static {v13}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    .line 371
    .line 372
    .line 373
    move-result-object v13

    .line 374
    const-string v14, "is_released"

    .line 375
    .line 376
    invoke-virtual {v1, v13, v14}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 377
    .line 378
    .line 379
    invoke-virtual {v1}, Lf/y03;->B7()V

    .line 380
    .line 381
    .line 382
    add-int/lit8 v10, v10, 0x1

    .line 383
    .line 384
    int-to-byte v10, v10

    .line 385
    goto :goto_129

    .line 386
    :cond_181
    invoke-virtual {v1}, Lf/y03;->DE()V

    .line 387
    .line 388
    .line 389
    :cond_184
    const-string v10, "evolutions"

    .line 390
    .line 391
    invoke-virtual {v1, v10}, Lf/y03;->SJ1(Ljava/lang/String;)V

    .line 392
    .line 393
    .line 394
    iget-object v10, v7, Lf/zp3;->tg:Ljava/util/ArrayList;

    .line 395
    .line 396
    invoke-virtual {v10}, Ljava/util/ArrayList;->size()I

    .line 397
    .line 398
    .line 399
    move-result v13

    .line 400
    const/4 v14, 0x0

    .line 401
    :goto_190
    const-string v15, "type"

    .line 402
    .line 403
    if-ge v14, v13, :cond_1ff

    .line 404
    .line 405
    invoke-virtual {v10, v14}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 406
    .line 407
    .line 408
    move-result-object v16

    .line 409
    add-int/lit8 v14, v14, 0x1

    .line 410
    .line 411
    move-object/from16 v9, v16

    .line 412
    .line 413
    check-cast v9, Lf/b81;

    .line 414
    .line 415
    invoke-virtual {v1}, Lf/y03;->mJ0()V

    .line 416
    .line 417
    .line 418
    iget-short v5, v9, Lf/b81;->P6:S

    .line 419
    .line 420
    move-object/from16 v17, v2

    .line 421
    .line 422
    iget v2, v9, Lf/b81;->zw0:I
    # MonMMO-EX: drop a data evolution's time-of-day bits (f/fi7 0x1000) from the dumped value.
    shl-int/lit8 v2, v2, 0x10
    ushr-int/lit8 v2, v2, 0x10

    .line 423
    .line 424
    move/from16 v18, v3

    .line 425
    .line 426
    iget-object v3, v9, Lf/b81;->uR0:Lf/vj3;

    .line 427
    .line 428
    invoke-static {v5}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 429
    .line 430
    .line 431
    move-result-object v5

    .line 432
    invoke-virtual {v1, v5, v11}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 433
    .line 434
    .line 435
    invoke-static {}, Lf/y91;->xk0()Lf/y91;

    .line 436
    .line 437
    .line 438
    move-result-object v5

    .line 439
    iget-short v9, v9, Lf/b81;->P6:S

    .line 440
    .line 441
    invoke-virtual {v5, v9}, Lf/y91;->wT0(S)Lf/zp3;

    .line 442
    .line 443
    .line 444
    move-result-object v5

    .line 445
    const/4 v9, 0x0

    .line 446
    invoke-virtual {v5, v9}, Lf/zp3;->xt(Z)Ljava/lang/String;

    .line 447
    .line 448
    .line 449
    move-result-object v5

    .line 450
    invoke-virtual {v1, v5, v12}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 451
    .line 452
    .line 453
    invoke-virtual {v3}, Ljava/lang/Enum;->name()Ljava/lang/String;

    .line 454
    .line 455
    .line 456
    move-result-object v5

    .line 457
    invoke-virtual {v1, v5, v15}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 458
    .line 459
    .line 460
    const-string v5, "val"

    .line 461
    .line 462
    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 463
    .line 464
    .line 465
    move-result-object v9

    .line 466
    invoke-virtual {v1, v9, v5}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 467
    .line 468
    .line 469
    invoke-virtual {v3}, Ljava/lang/Enum;->ordinal()I

    .line 470
    .line 471
    .line 472
    move-result v3

    .line 473
    const/4 v5, 0x6

    .line 474
    if-eq v3, v5, :cond_1e3

    .line 475
    .line 476
    const/16 v5, 0x8

    .line 477
    .line 478
    if-eq v3, v5, :cond_1e3

    .line 479
    .line 480
    packed-switch v3, :pswitch_data_4ac

    .line 481
    .line 482
    .line 483
    goto :goto_1f5

    .line 484
    :cond_1e3
    :pswitch_1e3
    sget-object v3, Lf/an8;->LU:Lf/an8;

    .line 485
    .line 486
    int-to-short v2, v2

    .line 487
    invoke-virtual {v3, v2}, Lf/an8;->R3(S)Lf/ls0;

    .line 488
    .line 489
    .line 490
    move-result-object v2

    .line 491
    iget v2, v2, Lf/ls0;->FU:I

    .line 492
    .line 493
    invoke-static {v2}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 494
    .line 495
    .line 496
    move-result-object v2

    .line 497
    const-string v3, "item_name"

    .line 498
    .line 499
    invoke-virtual {v1, v2, v3}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 500
    .line 501
    .line 502
    :goto_1f5
    invoke-virtual {v1}, Lf/y03;->B7()V

    .line 503
    .line 504
    .line 505
    move-object/from16 v2, v17

    .line 506
    .line 507
    move/from16 v3, v18

    .line 508
    .line 509
    const/4 v5, 0x0

    .line 510
    const/4 v9, 0x1

    .line 511
    goto :goto_190

    .line 512
    :cond_1ff
    move-object/from16 v17, v2

    .line 513
    .line 514
    move/from16 v18, v3

    .line 515
    .line 516
    invoke-virtual {v1}, Lf/y03;->DE()V

    .line 517
    .line 518
    .line 519
    const-string v2, "moves"

    .line 520
    .line 521
    invoke-virtual {v1, v2}, Lf/y03;->SJ1(Ljava/lang/String;)V

    .line 522
    .line 523
    .line 524
    iget-object v2, v7, Lf/zp3;->qy:Ljava/util/ArrayList;

    .line 525
    .line 526
    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I

    .line 527
    .line 528
    .line 529
    move-result v3

    .line 530
    const/4 v9, 0x0

    .line 531
    :goto_212
    if-ge v9, v3, :cond_24d

    .line 532
    .line 533
    invoke-virtual {v2, v9}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 534
    .line 535
    .line 536
    move-result-object v5

    .line 537
    add-int/lit8 v9, v9, 0x1

    .line 538
    .line 539
    check-cast v5, Lf/hj5;

    .line 540
    .line 541
    invoke-virtual {v1}, Lf/y03;->mJ0()V

    .line 542
    .line 543
    .line 544
    iget-short v10, v5, Lf/hj5;->Qh1:S

    .line 545
    .line 546
    invoke-static {v10}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 547
    .line 548
    .line 549
    move-result-object v10

    .line 550
    invoke-virtual {v1, v10, v11}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 551
    .line 552
    .line 553
    invoke-static {}, Lf/k92;->dh0()Lf/k92;

    .line 554
    .line 555
    .line 556
    move-result-object v10

    .line 557
    iget-short v13, v5, Lf/hj5;->Qh1:S

    .line 558
    .line 559
    invoke-virtual {v10, v13}, Lf/k92;->BW1(S)Lf/hu6;

    .line 560
    .line 561
    .line 562
    move-result-object v10

    .line 563
    iget v10, v10, Lf/hu6;->bl1:I

    .line 564
    .line 565
    invoke-static {v10}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 566
    .line 567
    .line 568
    move-result-object v10

    .line 569
    invoke-virtual {v1, v10, v12}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 570
    .line 571
    .line 572
    const-string v10, "level"

    .line 573
    .line 574
    invoke-virtual {v1, v10, v15}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 575
    .line 576
    .line 577
    iget-byte v5, v5, Lf/hj5;->fi0:B

    .line 578
    .line 579
    invoke-static {v5}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 580
    .line 581
    .line 582
    move-result-object v5

    .line 583
    invoke-virtual {v1, v5, v10}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 584
    .line 585
    .line 586
    invoke-virtual {v1}, Lf/y03;->B7()V

    .line 587
    .line 588
    .line 589
    goto :goto_212

    .line 590
    :cond_24d
    sget-object v2, Lf/rb8;->F01:[Lf/rb8;

    .line 591
    .line 592
    array-length v3, v2

    .line 593
    const/4 v9, 0x0

    .line 594
    :goto_251
    if-ge v9, v3, :cond_29a

    .line 595
    .line 596
    aget-object v5, v2, v9

    .line 597
    .line 598
    iget-object v10, v7, Lf/zp3;->KB:[[S

    .line 599
    .line 600
    iget-byte v13, v5, Lf/rb8;->G71:B

    .line 601
    .line 602
    aget-object v10, v10, v13

    .line 603
    .line 604
    array-length v13, v10

    .line 605
    const/4 v14, 0x0

    .line 606
    :goto_25d
    move-object/from16 v19, v2

    .line 607
    .line 608
    if-ge v14, v13, :cond_295

    .line 609
    .line 610
    aget-short v2, v10, v14

    .line 611
    .line 612
    invoke-virtual {v1}, Lf/y03;->mJ0()V

    .line 613
    .line 614
    .line 615
    move/from16 v20, v3

    .line 616
    .line 617
    invoke-static {v2}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 618
    .line 619
    .line 620
    move-result-object v3

    .line 621
    invoke-virtual {v1, v3, v11}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 622
    .line 623
    .line 624
    invoke-static {}, Lf/k92;->dh0()Lf/k92;

    .line 625
    .line 626
    .line 627
    move-result-object v3

    .line 628
    invoke-virtual {v3, v2}, Lf/k92;->BW1(S)Lf/hu6;

    .line 629
    .line 630
    .line 631
    move-result-object v2

    .line 632
    iget v2, v2, Lf/hu6;->bl1:I

    .line 633
    .line 634
    invoke-static {v2}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 635
    .line 636
    .line 637
    move-result-object v2

    .line 638
    invoke-virtual {v1, v2, v12}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 639
    .line 640
    .line 641
    iget-byte v2, v5, Lf/rb8;->G71:B

    .line 642
    .line 643
    add-int/lit16 v2, v2, 0x6d6

    .line 644
    .line 645
    invoke-static {v2}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 646
    .line 647
    .line 648
    move-result-object v2

    .line 649
    invoke-virtual {v1, v2, v15}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 650
    .line 651
    .line 652
    invoke-virtual {v1}, Lf/y03;->B7()V

    .line 653
    .line 654
    .line 655
    add-int/lit8 v14, v14, 0x1

    .line 656
    .line 657
    move-object/from16 v2, v19

    .line 658
    .line 659
    move/from16 v3, v20

    .line 660
    .line 661
    goto :goto_25d

    .line 662
    :cond_295
    move/from16 v20, v3

    .line 663
    .line 664
    add-int/lit8 v9, v9, 0x1

    .line 665
    .line 666
    goto :goto_251

    .line 667
    :cond_29a
    invoke-virtual {v1}, Lf/y03;->DE()V

    .line 668
    .line 669
    .line 670
    const-string v2, "types"

    .line 671
    .line 672
    invoke-virtual {v1, v2}, Lf/y03;->SJ1(Ljava/lang/String;)V

    .line 673
    .line 674
    .line 675
    const/4 v2, -0x1

    .line 676
    invoke-virtual {v7, v2}, Lf/zp3;->ef(B)Lf/eb6;

    .line 677
    .line 678
    .line 679
    move-result-object v3

    .line 680
    invoke-virtual {v3}, Ljava/lang/Object;->toString()Ljava/lang/String;

    .line 681
    .line 682
    .line 683
    move-result-object v3

    .line 684
    invoke-virtual {v1, v3}, Lf/y03;->dZ0(Ljava/lang/String;)V

    .line 685
    .line 686
    .line 687
    invoke-virtual {v7, v2}, Lf/zp3;->ch(B)Lf/eb6;

    .line 688
    .line 689
    .line 690
    move-result-object v2

    .line 691
    invoke-virtual {v2}, Ljava/lang/Object;->toString()Ljava/lang/String;

    .line 692
    .line 693
    .line 694
    move-result-object v2

    .line 695
    invoke-virtual {v1, v2}, Lf/y03;->dZ0(Ljava/lang/String;)V

    .line 696
    .line 697
    .line 698
    invoke-virtual {v1}, Lf/y03;->DE()V

    .line 699
    .line 700
    .line 701
    const-string v2, "stats"

    .line 702
    .line 703
    invoke-virtual {v1, v2}, Lf/y03;->Ta0(Ljava/lang/String;)V

    .line 704
    .line 705
    .line 706
    sget-object v2, Lf/r59;->Vu0:[Lf/r59;

    .line 707
    .line 708
    array-length v3, v2

    .line 709
    const/4 v9, 0x0

    .line 710
    :goto_2c5
    if-ge v9, v3, :cond_2df

    .line 711
    .line 712
    aget-object v5, v2, v9

    .line 713
    .line 714
    invoke-virtual {v5}, Ljava/lang/Enum;->name()Ljava/lang/String;

    .line 715
    .line 716
    .line 717
    move-result-object v10

    .line 718
    invoke-virtual {v10}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;

    .line 719
    .line 720
    .line 721
    move-result-object v10

    .line 722
    invoke-virtual {v7, v5}, Lf/zp3;->xA1(Lf/r59;)I

    .line 723
    .line 724
    .line 725
    move-result v5

    .line 726
    invoke-static {v5}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 727
    .line 728
    .line 729
    move-result-object v5

    .line 730
    invoke-virtual {v1, v5, v10}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 731
    .line 732
    .line 733
    add-int/lit8 v9, v9, 0x1

    .line 734
    .line 735
    goto :goto_2c5

    .line 736
    :cond_2df
    invoke-virtual {v1}, Lf/y03;->B7()V

    .line 737
    .line 738
    .line 739
    const-string v2, "yields"

    .line 740
    .line 741
    invoke-virtual {v1, v2}, Lf/y03;->Ta0(Ljava/lang/String;)V

    .line 742
    .line 743
    .line 744
    iget v2, v7, Lf/zp3;->pu1:I

    .line 745
    .line 746
    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 747
    .line 748
    .line 749
    move-result-object v2

    .line 750
    const-string v3, "exp"

    .line 751
    .line 752
    invoke-virtual {v1, v2, v3}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 753
    .line 754
    .line 755
    sget-object v2, Lf/r59;->Vu0:[Lf/r59;

    .line 756
    .line 757
    array-length v3, v2

    .line 758
    const/4 v9, 0x0

    .line 759
    :goto_2f6
    if-ge v9, v3, :cond_320

    .line 760
    .line 761
    aget-object v5, v2, v9

    .line 762
    .line 763
    new-instance v10, Ljava/lang/StringBuilder;

    .line 764
    .line 765
    const-string v13, "ev_"

    .line 766
    .line 767
    invoke-direct {v10, v13}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 768
    .line 769
    .line 770
    invoke-virtual {v5}, Ljava/lang/Enum;->name()Ljava/lang/String;

    .line 771
    .line 772
    .line 773
    move-result-object v13

    .line 774
    invoke-virtual {v13}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;

    .line 775
    .line 776
    .line 777
    move-result-object v13

    .line 778
    invoke-virtual {v10, v13}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 779
    .line 780
    .line 781
    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 782
    .line 783
    .line 784
    move-result-object v10

    .line 785
    iget-object v13, v7, Lf/zp3;->Hn1:[B

    .line 786
    .line 787
    iget-byte v5, v5, Lf/r59;->e:B

    .line 788
    .line 789
    aget-byte v5, v13, v5

    .line 790
    .line 791
    invoke-static {v5}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 792
    .line 793
    .line 794
    move-result-object v5

    .line 795
    invoke-virtual {v1, v5, v10}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 796
    .line 797
    .line 798
    add-int/lit8 v9, v9, 0x1

    .line 799
    .line 800
    goto :goto_2f6

    .line 801
    :cond_320
    invoke-virtual {v1}, Lf/y03;->B7()V

    .line 802
    .line 803
    .line 804
    const-string v2, "tiers"

    .line 805
    .line 806
    invoke-virtual {v1, v2}, Lf/y03;->SJ1(Ljava/lang/String;)V

    .line 807
    .line 808
    .line 809
    iget-object v2, v7, Lf/zp3;->Bu:Lf/ll2;

    .line 810
    .line 811
    iget v2, v2, Lf/ll2;->hS:I

    .line 812
    .line 813
    invoke-static {v2}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 814
    .line 815
    .line 816
    move-result-object v2

    .line 817
    invoke-virtual {v1, v2}, Lf/y03;->dZ0(Ljava/lang/String;)V

    .line 818
    .line 819
    .line 820
    iget-object v2, v7, Lf/zp3;->Ri0:Ljava/util/HashSet;

    .line 821
    .line 822
    invoke-virtual {v2}, Ljava/util/HashSet;->iterator()Ljava/util/Iterator;

    .line 823
    .line 824
    .line 825
    move-result-object v2

    .line 826
    :goto_339
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    .line 827
    .line 828
    .line 829
    move-result v3

    .line 830
    if-eqz v3, :cond_34f

    .line 831
    .line 832
    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 833
    .line 834
    .line 835
    move-result-object v3

    .line 836
    check-cast v3, Lf/ll2;

    .line 837
    .line 838
    iget v3, v3, Lf/ll2;->hS:I

    .line 839
    .line 840
    invoke-static {v3}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 841
    .line 842
    .line 843
    move-result-object v3

    .line 844
    invoke-virtual {v1, v3}, Lf/y03;->dZ0(Ljava/lang/String;)V

    .line 845
    .line 846
    .line 847
    goto :goto_339

    .line 848
    :cond_34f
    invoke-virtual {v1}, Lf/y03;->DE()V

    .line 849
    .line 850
    .line 851
    const-string v2, "held_items"

    .line 852
    .line 853
    invoke-virtual {v1, v2}, Lf/y03;->SJ1(Ljava/lang/String;)V

    .line 854
    .line 855
    .line 856
    iget-object v2, v7, Lf/zp3;->Tf1:[S

    .line 857
    .line 858
    array-length v3, v2

    .line 859
    const/4 v9, 0x0

    .line 860
    :goto_35b
    if-ge v9, v3, :cond_37e

    .line 861
    .line 862
    aget-short v5, v2, v9

    .line 863
    .line 864
    invoke-virtual {v1}, Lf/y03;->mJ0()V

    .line 865
    .line 866
    .line 867
    invoke-static {v5}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 868
    .line 869
    .line 870
    move-result-object v7

    .line 871
    invoke-virtual {v1, v7, v11}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 872
    .line 873
    .line 874
    sget-object v7, Lf/an8;->LU:Lf/an8;

    .line 875
    .line 876
    invoke-virtual {v7, v5}, Lf/an8;->R3(S)Lf/ls0;

    .line 877
    .line 878
    .line 879
    move-result-object v5

    .line 880
    iget v5, v5, Lf/ls0;->FU:I

    .line 881
    .line 882
    invoke-static {v5}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 883
    .line 884
    .line 885
    move-result-object v5

    .line 886
    invoke-virtual {v1, v5, v12}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 887
    .line 888
    .line 889
    invoke-virtual {v1}, Lf/y03;->B7()V

    .line 890
    .line 891
    .line 892
    add-int/lit8 v9, v9, 0x1

    .line 893
    .line 894
    goto :goto_35b

    .line 895
    :cond_37e
    invoke-virtual {v1}, Lf/y03;->DE()V

    .line 896
    .line 897
    .line 898
    const-string v2, "locations"

    .line 899
    .line 900
    invoke-virtual {v1, v2}, Lf/y03;->SJ1(Ljava/lang/String;)V

    .line 901
    .line 902
    .line 903
    sget-object v2, Lf/fi8;->PC:Lf/fi8;

    .line 904
    .line 905
    iget-object v2, v2, Lf/fi8;->Wg0:Lf/ch4;

    .line 906
    .line 907
    invoke-virtual {v2, v8}, Lf/ch4;->xH0(S)Ljava/lang/Object;

    .line 908
    .line 909
    .line 910
    move-result-object v2

    .line 911
    check-cast v2, Ljava/util/List;

    .line 912
    .line 913
    if-nez v2, :cond_394

    .line 914
    .line 915
    sget-object v2, Ljava/util/Collections;->EMPTY_LIST:Ljava/util/List;

    .line 916
    .line 917
    :cond_394
    invoke-static {v2}, Lj$/util/Collection$-EL;->stream(Ljava/util/Collection;)Lj$/util/stream/Stream;

    .line 918
    .line 919
    .line 920
    move-result-object v2

    .line 921
    invoke-interface {v2}, Lj$/util/stream/Stream;->sorted()Lj$/util/stream/Stream;

    .line 922
    .line 923
    .line 924
    move-result-object v2

    .line 925
    invoke-static {}, Lj$/util/stream/Collectors;->toList()Lj$/util/stream/Collector;

    .line 926
    .line 927
    .line 928
    move-result-object v3

    .line 929
    invoke-interface {v2, v3}, Lj$/util/stream/Stream;->collect(Lj$/util/stream/Collector;)Ljava/lang/Object;

    .line 930
    .line 931
    .line 932
    move-result-object v2

    .line 933
    check-cast v2, Ljava/util/List;

    .line 934
    .line 935
    invoke-interface {v2}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    .line 936
    .line 937
    .line 938
    move-result-object v2

    .line 939
    :goto_3aa
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    .line 940
    .line 941
    .line 942
    move-result v3

    .line 943
    if-eqz v3, :cond_474

    .line 944
    .line 945
    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 946
    .line 947
    .line 948
    move-result-object v3

    .line 949
    check-cast v3, Lf/nl2;

    .line 950
    .line 951
    invoke-virtual {v1}, Lf/y03;->mJ0()V

    .line 952
    .line 953
    .line 954
    iget-byte v5, v3, Lf/nl2;->FD0:B

    .line 955
    .line 956
    invoke-static {v5}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 957
    .line 958
    .line 959
    move-result-object v5

    .line 960
    const-string v7, "form"

    .line 961
    .line 962
    invoke-virtual {v1, v5, v7}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 963
    .line 964
    .line 965
    invoke-virtual {v3}, Lf/nl2;->EB1()Ljava/lang/String;

    .line 966
    .line 967
    .line 968
    move-result-object v5

    .line 969
    invoke-virtual {v1, v5, v15}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 970
    .line 971
    .line 972
    iget-byte v5, v3, Lf/nl2;->iC:B

    .line 973
    .line 974
    invoke-static {v5}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 975
    .line 976
    .line 977
    move-result-object v7

    .line 978
    const-string v8, "region_id"

    .line 979
    .line 980
    invoke-virtual {v1, v7, v8}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 981
    .line 982
    .line 983
    const v7, 0x3d090

    .line 984
    .line 985
    .line 986
    add-int/2addr v5, v7

    .line 987
    invoke-static {v5}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 988
    .line 989
    .line 990
    move-result-object v5

    .line 991
    const-string v7, "region_name"

    .line 992
    .line 993
    invoke-virtual {v1, v5, v7}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 994
    .line 995
    .line 996
    iget-short v5, v3, Lf/nl2;->A10:S

    .line 997
    .line 998
    invoke-static {v5}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 999
    .line 1000
    .line 1001
    move-result-object v5

    .line 1002
    const-string v7, "location_id"

    .line 1003
    .line 1004
    invoke-virtual {v1, v5, v7}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 1005
    .line 1006
    .line 1007
    const-string v5, "location_name"

    .line 1008
    .line 1009
    const/4 v9, 0x0

    .line 1010
    invoke-virtual {v3, v9}, Lf/nl2;->F01(Z)Ljava/lang/String;

    .line 1011
    .line 1012
    .line 1013
    move-result-object v7

    .line 1014
    invoke-virtual {v1, v7, v5}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 1015
    .line 1016
    .line 1017
    const-string v5, "location_name_full"

    .line 1018
    .line 1019
    const/4 v7, 0x1

    .line 1020
    invoke-virtual {v3, v7}, Lf/nl2;->F01(Z)Ljava/lang/String;

    .line 1021
    .line 1022
    .line 1023
    move-result-object v8

    .line 1024
    invoke-virtual {v1, v8, v5}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 1025
    .line 1026
    .line 1027
    iget-byte v5, v3, Lf/nl2;->VP1:B

    .line 1028
    .line 1029
    invoke-static {v5}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 1030
    .line 1031
    .line 1032
    move-result-object v5

    .line 1033
    const-string v8, "min_level"

    .line 1034
    .line 1035
    invoke-virtual {v1, v5, v8}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 1036
    .line 1037
    .line 1038
    iget-byte v5, v3, Lf/nl2;->ZQ0:B

    .line 1039
    .line 1040
    invoke-static {v5}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 1041
    .line 1042
    .line 1043
    move-result-object v5

    .line 1044
    const-string v8, "max_level"

    .line 1045
    .line 1046
    invoke-virtual {v1, v5, v8}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 1047
    .line 1048
    .line 1049
    iget-object v5, v3, Lf/nl2;->AI1:Lf/b38;

    .line 1050
    .line 1051
    invoke-virtual {v5}, Lf/b38;->toString()Ljava/lang/String;

    .line 1052
    .line 1053
    .line 1054
    move-result-object v5

    .line 1055
    const-string v8, "season"

    .line 1056
    .line 1057
    invoke-virtual {v1, v5, v8}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 1058
    .line 1059
    .line 1060
    const/16 v5, 0x200

    .line 1061
    .line 1062
    invoke-virtual {v3, v5}, Lf/nl2;->uM0(S)Z

    .line 1063
    .line 1064
    .line 1065
    move-result v5

    .line 1066
    invoke-static {v5}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    .line 1067
    .line 1068
    .line 1069
    move-result-object v5

    .line 1070
    const-string v8, "is_horde_3x"

    .line 1071
    .line 1072
    invoke-virtual {v1, v5, v8}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 1073
    .line 1074
    .line 1075
    const/16 v5, 0x400

    .line 1076
    .line 1077
    invoke-virtual {v3, v5}, Lf/nl2;->uM0(S)Z

    .line 1078
    .line 1079
    .line 1080
    move-result v5

    .line 1081
    invoke-static {v5}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    .line 1082
    .line 1083
    .line 1084
    move-result-object v5

    .line 1085
    const-string v8, "is_horde_5x"

    .line 1086
    .line 1087
    invoke-virtual {v1, v5, v8}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 1088
    .line 1089
    .line 1090
    iget-short v5, v3, Lf/nl2;->Nc0:S

    .line 1091
    .line 1092
    invoke-static {v5}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 1093
    .line 1094
    .line 1095
    move-result-object v5

    .line 1096
    const-string v8, "rarity_flags"

    .line 1097
    .line 1098
    invoke-virtual {v1, v5, v8}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 1099
    .line 1100
    .line 1101
    sget-object v5, Lf/h11;->hw1:Lf/h11;

    .line 1102
    .line 1103
    sget-object v8, Lf/xf0;->wx:Lf/yw7;

    .line 1104
    .line 1105
    invoke-virtual {v3, v5, v8}, Lf/nl2;->X00(Lf/h11;Lf/yw7;)Ljava/lang/String;

    .line 1106
    .line 1107
    .line 1108
    move-result-object v5

    .line 1109
    const-string v10, "rarity_morning"

    .line 1110
    .line 1111
    invoke-virtual {v1, v5, v10}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 1112
    .line 1113
    .line 1114
    sget-object v5, Lf/h11;->kP:Lf/h11;

    .line 1115
    .line 1116
    invoke-virtual {v3, v5, v8}, Lf/nl2;->X00(Lf/h11;Lf/yw7;)Ljava/lang/String;

    .line 1117
    .line 1118
    .line 1119
    move-result-object v5

    .line 1120
    const-string v10, "rarity_day"

    .line 1121
    .line 1122
    invoke-virtual {v1, v5, v10}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 1123
    .line 1124
    .line 1125
    sget-object v5, Lf/h11;->DM0:Lf/h11;

    .line 1126
    .line 1127
    invoke-virtual {v3, v5, v8}, Lf/nl2;->X00(Lf/h11;Lf/yw7;)Ljava/lang/String;

    .line 1128
    .line 1129
    .line 1130
    move-result-object v3

    .line 1131
    const-string v5, "rarity_night"

    .line 1132
    .line 1133
    invoke-virtual {v1, v3, v5}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 1134
    .line 1135
    .line 1136
    invoke-virtual {v1}, Lf/y03;->B7()V

    .line 1137
    .line 1138
    .line 1139
    goto/16 :goto_3aa

    .line 1140
    .line 1141
    :cond_474
    const/4 v9, 0x0

    .line 1142
    invoke-virtual {v1}, Lf/y03;->DE()V

    .line 1143
    .line 1144
    .line 1145
    invoke-virtual {v1}, Lf/y03;->B7()V

    .line 1146
    .line 1147
    .line 1148
    move-object/from16 v2, v17

    .line 1149
    .line 1150
    move/from16 v3, v18

    .line 1151
    .line 1152
    const/4 v5, 0x0

    .line 1153
    goto/16 :goto_3d

    .line 1154
    .line 1155
    :cond_482
    invoke-virtual {v1}, Lf/y03;->DE()V

    .line 1156
    .line 1157
    .line 1158
    iget-object v2, v1, Lf/y03;->O30:Lf/om3;

    .line 1159
    .line 1160
    iget-object v2, v2, Lf/om3;->Zj1:Ljava/io/Writer;

    .line 1161
    .line 1162
    invoke-virtual {v2}, Ljava/lang/Object;->toString()Ljava/lang/String;

    .line 1163
    .line 1164
    .line 1165
    move-result-object v2

    .line 1166
    invoke-virtual {v1, v2}, Lf/y03;->PX(Ljava/lang/String;)Ljava/lang/String;

    .line 1167
    .line 1168
    .line 1169
    move-result-object v1

    .line 1170
    new-instance v2, Ljava/util/zip/ZipEntry;

    .line 1171
    .line 1172
    const-string v3, "info/monsters.json"

    .line 1173
    .line 1174
    invoke-direct {v2, v3}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 1175
    .line 1176
    .line 1177
    invoke-virtual {v0, v2}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 1178
    .line 1179
    .line 1180
    sget-object v2, Ljava/nio/charset/StandardCharsets;->UTF_8:Ljava/nio/charset/Charset;

    .line 1181
    .line 1182
    invoke-virtual {v1, v2}, Ljava/lang/String;->getBytes(Ljava/nio/charset/Charset;)[B

    .line 1183
    .line 1184
    .line 1185
    move-result-object v1

    .line 1186
    invoke-virtual {v0, v1}, Ljava/io/OutputStream;->write([B)V

    .line 1187
    .line 1188
    .line 1189
    invoke-virtual {v0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 1190
    .line 1191
    .line 1192
    invoke-virtual {v4}, Lf/om3;->close()V

    .line 1193
    .line 1194
    .line 1195
    return-void

    .line 1196
    nop

    .line 1197
    :pswitch_data_4ac
    .packed-switch 0x11
        :pswitch_1e3
        :pswitch_1e3
        :pswitch_1e3
        :pswitch_1e3
    .end packed-switch
.end method

.method public static vj1(Lf/n15;)V
    .registers 6

    .line 1
    new-instance v0, Ljava/util/zip/ZipEntry;

    .line 2
    .line 3
    const-string v1, "info.xml"

    .line 4
    .line 5
    invoke-direct {v0, v1}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 6
    .line 7
    .line 8
    invoke-virtual {p0, v0}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 9
    .line 10
    .line 11
    invoke-static {}, Ljavax/xml/parsers/DocumentBuilderFactory;->newInstance()Ljavax/xml/parsers/DocumentBuilderFactory;

    .line 12
    .line 13
    .line 14
    move-result-object v0

    .line 15
    invoke-virtual {v0}, Ljavax/xml/parsers/DocumentBuilderFactory;->newDocumentBuilder()Ljavax/xml/parsers/DocumentBuilder;

    .line 16
    .line 17
    .line 18
    move-result-object v0

    .line 19
    invoke-virtual {v0}, Ljavax/xml/parsers/DocumentBuilder;->newDocument()Lorg/w3c/dom/Document;

    .line 20
    .line 21
    .line 22
    move-result-object v0

    .line 23
    const-string v1, "resource"

    .line 24
    .line 25
    invoke-interface {v0, v1}, Lorg/w3c/dom/Document;->createElement(Ljava/lang/String;)Lorg/w3c/dom/Element;

    .line 26
    .line 27
    .line 28
    move-result-object v1

    .line 29
    const-string v2, "name"

    .line 30
    .line 31
    const-string v3, "Dumped Resources"

    .line 32
    .line 33
    invoke-interface {v1, v2, v3}, Lorg/w3c/dom/Element;->setAttribute(Ljava/lang/String;Ljava/lang/String;)V

    .line 34
    .line 35
    .line 36
    const-string v2, "version"

    .line 37
    .line 38
    const-string v3, "0.0"

    .line 39
    .line 40
    invoke-interface {v1, v2, v3}, Lorg/w3c/dom/Element;->setAttribute(Ljava/lang/String;Ljava/lang/String;)V

    .line 41
    .line 42
    .line 43
    const-string v2, "description"

    .line 44
    .line 45
    const-string v3, "Directly dumped resources."

    .line 46
    .line 47
    invoke-interface {v1, v2, v3}, Lorg/w3c/dom/Element;->setAttribute(Ljava/lang/String;Ljava/lang/String;)V

    .line 48
    .line 49
    .line 50
    const-string v2, "author"

    .line 51
    .line 52
    const-string v3, "--"

    .line 53
    .line 54
    invoke-interface {v1, v2, v3}, Lorg/w3c/dom/Element;->setAttribute(Ljava/lang/String;Ljava/lang/String;)V

    .line 55
    .line 56
    .line 57
    const-string v2, "weblink"

    .line 58
    .line 59
    const-string v3, ""

    .line 60
    .line 61
    invoke-interface {v1, v2, v3}, Lorg/w3c/dom/Element;->setAttribute(Ljava/lang/String;Ljava/lang/String;)V

    .line 62
    .line 63
    .line 64
    invoke-interface {v0, v1}, Lorg/w3c/dom/Node;->appendChild(Lorg/w3c/dom/Node;)Lorg/w3c/dom/Node;

    .line 65
    .line 66
    .line 67
    invoke-static {}, Ljavax/xml/transform/TransformerFactory;->newInstance()Ljavax/xml/transform/TransformerFactory;

    .line 68
    .line 69
    .line 70
    move-result-object v1

    .line 71
    invoke-virtual {v1}, Ljavax/xml/transform/TransformerFactory;->newTransformer()Ljavax/xml/transform/Transformer;

    .line 72
    .line 73
    .line 74
    move-result-object v1

    .line 75
    new-instance v2, Ljavax/xml/transform/dom/DOMSource;

    .line 76
    .line 77
    invoke-direct {v2, v0}, Ljavax/xml/transform/dom/DOMSource;-><init>(Lorg/w3c/dom/Node;)V

    .line 78
    .line 79
    .line 80
    new-instance v0, Ljavax/xml/transform/stream/StreamResult;

    .line 81
    .line 82
    invoke-direct {v0, p0}, Ljavax/xml/transform/stream/StreamResult;-><init>(Ljava/io/OutputStream;)V

    .line 83
    .line 84
    .line 85
    const-string v3, "indent"

    .line 86
    .line 87
    const-string v4, "yes"

    .line 88
    .line 89
    invoke-virtual {v1, v3, v4}, Ljavax/xml/transform/Transformer;->setOutputProperty(Ljava/lang/String;Ljava/lang/String;)V

    .line 90
    .line 91
    .line 92
    const-string v3, "{http://xml.apache.org/xslt}indent-amount"

    .line 93
    .line 94
    const-string v4, "4"

    .line 95
    .line 96
    invoke-virtual {v1, v3, v4}, Ljavax/xml/transform/Transformer;->setOutputProperty(Ljava/lang/String;Ljava/lang/String;)V

    .line 97
    .line 98
    .line 99
    invoke-virtual {v1, v2, v0}, Ljavax/xml/transform/Transformer;->transform(Ljavax/xml/transform/Source;Ljavax/xml/transform/Result;)V

    .line 100
    .line 101
    .line 102
    invoke-virtual {p0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 103
    .line 104
    .line 105
    return-void
.end method

.method public static zp1(Lf/n15;)V
    .registers 8

    .line 1
    new-instance v0, Lf/y03;

    .line 2
    .line 3
    sget-object v1, Lf/om3$w38;->Sn1:Lf/om3$w38;

    .line 4
    .line 5
    invoke-direct {v0, v1}, Lf/y03;-><init>(Lf/om3$w38;)V

    .line 6
    .line 7
    .line 8
    new-instance v2, Ljava/io/StringWriter;

    .line 9
    .line 10
    invoke-direct {v2}, Ljava/io/StringWriter;-><init>()V

    .line 11
    .line 12
    .line 13
    new-instance v3, Lf/om3;

    .line 14
    .line 15
    invoke-direct {v3, v2}, Lf/om3;-><init>(Ljava/io/Writer;)V

    .line 16
    .line 17
    .line 18
    iput-object v1, v0, Lf/y03;->g90:Lf/om3$w38;

    .line 19
    .line 20
    invoke-virtual {v0, v3}, Lf/y03;->Sd(Ljava/io/Writer;)V

    .line 21
    .line 22
    .line 23
    invoke-virtual {v0}, Lf/y03;->Ou0()V

    .line 24
    .line 25
    .line 26
    new-instance v1, Ljava/util/ArrayList;

    .line 27
    .line 28
    sget-object v2, Lf/an8;->LU:Lf/an8;

    .line 29
    .line 30
    iget-object v2, v2, Lf/an8;->lO:Ljava/util/TreeMap;

    .line 31
    .line 32
    invoke-virtual {v2}, Ljava/util/TreeMap;->values()Ljava/util/Collection;

    .line 33
    .line 34
    .line 35
    move-result-object v2

    .line 36
    invoke-direct {v1, v2}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V

    .line 37
    .line 38
    .line 39
    new-instance v2, Lf/l43;

    .line 40
    .line 41
    const/16 v3, 0x11

    .line 42
    .line 43
    invoke-direct {v2, v3}, Lf/l43;-><init>(I)V

    .line 44
    .line 45
    .line 46
    invoke-static {v2}, Lj$/util/Comparator$-CC;->comparingInt(Ljava/util/function/ToIntFunction;)Ljava/util/Comparator;

    .line 47
    .line 48
    .line 49
    move-result-object v2

    .line 50
    invoke-static {v1, v2}, Ljava/util/Collections;->sort(Ljava/util/List;Ljava/util/Comparator;)V

    .line 51
    .line 52
    .line 53
    invoke-virtual {v1}, Ljava/util/ArrayList;->size()I

    .line 54
    .line 55
    .line 56
    move-result v2

    .line 57
    const/4 v3, 0x0

    .line 58
    :goto_39
    if-ge v3, v2, :cond_9d

    .line 59
    .line 60
    invoke-virtual {v1, v3}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    .line 61
    .line 62
    .line 63
    move-result-object v4

    .line 64
    add-int/lit8 v3, v3, 0x1

    .line 65
    .line 66
    check-cast v4, Lf/ls0;

    .line 67
    .line 68
    iget-short v5, v4, Lf/ls0;->C4:S

    .line 69
    .line 70
    const/4 v6, 0x1

    .line 71
    if-ge v5, v6, :cond_49

    .line 72
    .line 73
    goto :goto_39

    .line 74
    :cond_49
    invoke-virtual {v0}, Lf/y03;->mJ0()V

    .line 75
    .line 76
    .line 77
    iget-short v5, v4, Lf/ls0;->C4:S

    .line 78
    .line 79
    invoke-static {v5}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 80
    .line 81
    .line 82
    move-result-object v5

    .line 83
    const-string v6, "id"

    .line 84
    .line 85
    invoke-virtual {v0, v5, v6}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 86
    .line 87
    .line 88
    iget v5, v4, Lf/ls0;->FU:I

    .line 89
    .line 90
    invoke-static {v5}, Lf/gt0;->lPT8(I)Ljava/lang/String;

    .line 91
    .line 92
    .line 93
    move-result-object v5

    .line 94
    const-string v6, "name"

    .line 95
    .line 96
    invoke-virtual {v0, v5, v6}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 97
    .line 98
    .line 99
    const-string v5, "desc"

    .line 100
    .line 101
    invoke-virtual {v4}, Lf/ls0;->tW1()Ljava/lang/String;

    .line 102
    .line 103
    .line 104
    move-result-object v6

    .line 105
    invoke-virtual {v0, v6, v5}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 106
    .line 107
    .line 108
    iget-byte v5, v4, Lf/ls0;->Lg0:B

    .line 109
    .line 110
    invoke-static {v5}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 111
    .line 112
    .line 113
    move-result-object v5

    .line 114
    const-string v6, "region_id"

    .line 115
    .line 116
    invoke-virtual {v0, v5, v6}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 117
    .line 118
    .line 119
    invoke-virtual {v4}, Lf/ls0;->Vi1()S

    .line 120
    .line 121
    .line 122
    move-result v5

    .line 123
    invoke-static {v5}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 124
    .line 125
    .line 126
    move-result-object v5

    .line 127
    const-string v6, "icon_id"

    .line 128
    .line 129
    invoke-virtual {v0, v5, v6}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 130
    .line 131
    .line 132
    iget v5, v4, Lf/ls0;->FU:I

    .line 133
    .line 134
    invoke-static {v5}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 135
    .line 136
    .line 137
    move-result-object v5

    .line 138
    const-string v6, "name_string_id"

    .line 139
    .line 140
    invoke-virtual {v0, v5, v6}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 141
    .line 142
    .line 143
    iget v4, v4, Lf/ls0;->Dt:I

    .line 144
    .line 145
    invoke-static {v4}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 146
    .line 147
    .line 148
    move-result-object v4

    .line 149
    const-string v5, "desc_string_id"

    .line 150
    .line 151
    invoke-virtual {v0, v4, v5}, Lf/y03;->wx(Ljava/lang/Object;Ljava/lang/String;)V

    .line 152
    .line 153
    .line 154
    invoke-virtual {v0}, Lf/y03;->B7()V

    .line 155
    .line 156
    .line 157
    goto :goto_39

    .line 158
    :cond_9d
    invoke-virtual {v0}, Lf/y03;->DE()V

    .line 159
    .line 160
    .line 161
    iget-object v1, v0, Lf/y03;->O30:Lf/om3;

    .line 162
    .line 163
    iget-object v1, v1, Lf/om3;->Zj1:Ljava/io/Writer;

    .line 164
    .line 165
    invoke-virtual {v1}, Ljava/lang/Object;->toString()Ljava/lang/String;

    .line 166
    .line 167
    .line 168
    move-result-object v1

    .line 169
    invoke-virtual {v0, v1}, Lf/y03;->PX(Ljava/lang/String;)Ljava/lang/String;

    .line 170
    .line 171
    .line 172
    move-result-object v0

    .line 173
    new-instance v1, Ljava/util/zip/ZipEntry;

    .line 174
    .line 175
    const-string v2, "info/items.json"

    .line 176
    .line 177
    invoke-direct {v1, v2}, Ljava/util/zip/ZipEntry;-><init>(Ljava/lang/String;)V

    .line 178
    .line 179
    .line 180
    invoke-virtual {p0, v1}, Ljava/util/zip/ZipOutputStream;->putNextEntry(Ljava/util/zip/ZipEntry;)V

    .line 181
    .line 182
    .line 183
    sget-object v1, Ljava/nio/charset/StandardCharsets;->UTF_8:Ljava/nio/charset/Charset;

    .line 184
    .line 185
    invoke-virtual {v0, v1}, Ljava/lang/String;->getBytes(Ljava/nio/charset/Charset;)[B

    .line 186
    .line 187
    .line 188
    move-result-object v0

    .line 189
    invoke-virtual {p0, v0}, Ljava/io/OutputStream;->write([B)V

    .line 190
    .line 191
    .line 192
    invoke-virtual {p0}, Ljava/util/zip/ZipOutputStream;->closeEntry()V

    .line 193
    .line 194
    .line 195
    return-void
.end method
