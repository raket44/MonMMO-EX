.class public abstract Lf/aw3;
.super Ljava/lang/Object;


# static fields
.field public static final YG:F

.field public static final pC1:Lf/xv7;


# instance fields
.field public final AZ:Ljava/util/ArrayList;

.field public Lz:Lf/al;

.field public final SE0:Lf/k89;

.field public final oT:Lf/wj4;

.field public final rh1:Lf/z74;

.field public zv0:Z


# direct methods
.method static constructor <clinit>()V
    .registers 1

    .line 1
    const-class v0, Lf/aw3;

    .line 2
    .line 3
    invoke-static {v0}, Lf/tv7;->I80(Ljava/lang/Class;)Lf/xv7;

    .line 4
    .line 5
    .line 6
    move-result-object v0

    .line 7
    sput-object v0, Lf/aw3;->pC1:Lf/xv7;

    .line 8
    .line 9
    const v0, 0x3a83126f    # 0.001f

    .line 10
    .line 11
    .line 12
    sput v0, Lf/aw3;->YG:F

    .line 13
    .line 14
    return-void
.end method

.method public constructor <init>()V
    .registers 4

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 2
    .line 3
    .line 4
    new-instance v0, Lf/z74;

    .line 5
    .line 6
    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    .line 7
    .line 8
    .line 9
    sget-wide v1, Lf/ja9;->sW:J

    .line 10
    .line 11
    iput-wide v1, v0, Lf/z74;->mj1:J

    .line 12
    .line 13
    new-instance v1, Ljava/util/ArrayList;

    .line 14
    .line 15
    invoke-direct {v1}, Ljava/util/ArrayList;-><init>()V

    .line 16
    .line 17
    .line 18
    invoke-static {v1}, Lj$/util/DesugarCollections;->synchronizedList(Ljava/util/List;)Ljava/util/List;

    .line 19
    .line 20
    .line 21
    move-result-object v1

    .line 22
    iput-object v1, v0, Lf/z74;->MG0:Ljava/util/List;

    .line 23
    .line 24
    iput-object v0, p0, Lf/aw3;->rh1:Lf/z74;

    .line 25
    .line 26
    new-instance v0, Lf/k89;

    .line 27
    .line 28
    invoke-direct {v0}, Lf/x44;-><init>()V

    .line 29
    .line 30
    .line 31
    iput-object v0, p0, Lf/aw3;->SE0:Lf/k89;

    .line 32
    .line 33
    const/4 v0, 0x0

    .line 34
    iput-object v0, p0, Lf/aw3;->Lz:Lf/al;

    .line 35
    .line 36
    new-instance v0, Ljava/util/ArrayList;

    .line 37
    .line 38
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 39
    .line 40
    .line 41
    iput-object v0, p0, Lf/aw3;->AZ:Ljava/util/ArrayList;

    .line 42
    .line 43
    const/4 v0, 0x0

    .line 44
    iput-boolean v0, p0, Lf/aw3;->zv0:Z

    .line 45
    .line 46
    new-instance v0, Lf/wj4;

    .line 47
    .line 48
    const/4 v1, 0x1

    .line 49
    invoke-direct {v0, v1}, Lf/wj4;-><init>(I)V

    .line 50
    .line 51
    .line 52
    iput-object v0, p0, Lf/aw3;->oT:Lf/wj4;

    .line 53
    .line 54
    return-void
.end method

.method public static lpT3(Lf/ni6;)F
    .registers 5

    .line 1
    sget-object v0, Lf/p37;->Pc0:Lf/li;

    .line 2
    .line 3
    const/high16 v1, 0x3f800000    # 1.0f

    .line 4
    .line 5
    if-nez v0, :cond_7

    .line 6
    .line 7
    goto :goto_4e

    .line 8
    :cond_7
    iget-object v0, v0, Lf/li;->I5:Lf/rt0;

    .line 9
    .line 10
    if-nez v0, :cond_c

    .line 11
    .line 12
    goto :goto_4e

    .line 13
    :cond_c
    if-eqz p0, :cond_4e

    .line 14
    .line 15
    iget-object v2, p0, Lf/t78;->iE:Ljava/lang/Object;

    .line 16
    .line 17
    check-cast v2, Lf/nw0;

    .line 18
    .line 19
    iget-object v3, v0, Lf/t78;->iE:Ljava/lang/Object;

    .line 20
    .line 21
    check-cast v3, Lf/nw0;

    .line 22
    .line 23
    invoke-virtual {v2, v3}, Lf/nw0;->equals(Ljava/lang/Object;)Z

    .line 24
    .line 25
    .line 26
    move-result v2

    .line 27
    if-eqz v2, :cond_1d

    .line 28
    .line 29
    goto :goto_4e

    .line 30
    :cond_1d
    sget-boolean v2, Lf/ms5;->m60:Z

    .line 31
    .line 32
    if-eqz v2, :cond_23

    .line 33
    .line 34
    const/4 p0, 0x0

    .line 35
    return p0

    .line 36
    :cond_23
    invoke-virtual {p0}, Lf/ni6;->vl1()Lf/rp3;

    .line 37
    .line 38
    .line 39
    move-result-object v2

    .line 40
    iget-object v2, v2, Lf/rp3;->qT1:Lf/u07;

    .line 41
    .line 42
    iget-object v0, v0, Lf/rt0;->iH0:Lf/rp3;

    .line 43
    .line 44
    iget-object v0, v0, Lf/rp3;->qT1:Lf/u07;

    .line 45
    .line 46
    invoke-virtual {v2, v0}, Lf/u07;->B40(Lf/u07;)F

    .line 47
    .line 48
    .line 49
    move-result v0

    .line 50
    iget-object p0, p0, Lf/ni6;->YO:Lf/jd4;

    .line 51
    .line 52
    iget-byte p0, p0, Lf/jd4;->oD0:B

    .line 53
    .line 54
    invoke-static {p0}, Lf/o80;->eF1(B)Z

    .line 55
    .line 56
    .line 57
    move-result p0

    .line 58
    if-eqz p0, :cond_40

    .line 59
    .line 60
    const/high16 p0, 0x40800000    # 4.0f

    .line 61
    .line 62
    mul-float v0, v0, p0

    .line 63
    .line 64
    goto :goto_43

    .line 65
    :cond_40
    const/high16 p0, 0x41800000    # 16.0f

    .line 66
    .line 67
    div-float/2addr v0, p0

    .line 68
    :goto_43
    const/high16 p0, 0x41880000    # 17.0f

    .line 69
    .line 70
    sub-float/2addr p0, v0

    .line 71
    const/high16 v0, 0x41700000    # 15.0f

    .line 72
    .line 73
    div-float/2addr p0, v0

    .line 74
    invoke-static {v1, p0}, Ljava/lang/Math;->min(FF)F

    .line 75
    .line 76
    .line 77
    move-result p0

    .line 78
    return p0

    .line 79
    :cond_4e
    :goto_4e
    return v1
.end method


# virtual methods
.method public final AI0()V
    .registers 4

    .line 1
    sget-object v0, Lf/dq7;->Et:Lf/nx3;

    .line 2
    .line 3
    new-instance v1, Lf/nb8;

    .line 4
    .line 5
    const/4 v2, 0x2

    .line 6
    invoke-direct {v1, p0, v2}, Lf/nb8;-><init>(Lf/aw3;I)V

    .line 7
    .line 8
    .line 9
    invoke-virtual {v0, v1}, Lf/nx3;->d3(Ljava/lang/Runnable;)V

    .line 10
    .line 11
    .line 12
    return-void
.end method

.method public final BG0(BS)V
    .registers 6

    .line 1
    sget-object v0, Lf/dq7;->Et:Lf/nx3;

    .line 2
    .line 3
    new-instance v1, Lf/a11;

    .line 4
    .line 5
    const/4 v2, 0x1

    .line 6
    invoke-direct {v1, p0, p1, p2, v2}, Lf/a11;-><init>(Ljava/lang/Object;BSI)V

    .line 7
    .line 8
    .line 9
    invoke-virtual {v0, v1}, Lf/nx3;->d3(Ljava/lang/Runnable;)V

    .line 10
    .line 11
    .line 12
    return-void
.end method

.method public final DR1(BSSLf/fu0;)Lf/al;
    .registers 21

    .line 1
    move-object/from16 v1, p0

    .line 2
    .line 3
    move/from16 v2, p1

    .line 4
    .line 5
    move/from16 v3, p2

    .line 6
    .line 7
    move-object/from16 v4, p4

    .line 8
    .line 9
    const-string v5, "Unable to load mod for {} {}"

    .line 10
    .line 11
    invoke-virtual {v4}, Lf/fu0;->Fi1()F

    .line 12
    .line 13
    .line 14
    move-result v6

    .line 15
    sget v0, Lf/aw3;->YG:F

    .line 16
    .line 17
    cmpg-float v0, v6, v0

    .line 18
    .line 19
    if-gtz v0, :cond_1a

    .line 20
    .line 21
    new-instance v0, Lf/w47;

    .line 22
    .line 23
    invoke-direct {v0, v2, v3}, Lf/w47;-><init>(BS)V

    .line 24
    .line 25
    .line 26
    return-object v0

    .line 27
    :cond_1a
    const/4 v7, 0x1

    .line 28
    if-lt v3, v7, :cond_18b

    .line 29
    .line 30
    const/16 v0, 0xa

    .line 31
    .line 32
    if-eq v2, v0, :cond_29

    .line 33
    .line 34
    sget-object v0, Lf/p37;->T10:Lf/zw0;

    .line 35
    .line 36
    invoke-virtual {v0, v2}, Lf/zw0;->Oe(B)Z

    .line 37
    .line 38
    .line 39
    move-result v0

    .line 40
    if-eqz v0, :cond_18b

    .line 41
    .line 42
    :cond_29
    const/16 v0, 0x7cf

    .line 43
    .line 44
    if-ne v3, v0, :cond_31

    .line 45
    .line 46
    if-ne v2, v7, :cond_31

    .line 47
    .line 48
    goto/16 :goto_18b

    .line 49
    .line 50
    :cond_31
    const/high16 v0, 0x10000

    .line 51
    .line 52
    mul-int v0, v0, v2

    .line 53
    .line 54
    add-int/2addr v0, v3

    .line 55
    iget-object v8, v1, Lf/aw3;->SE0:Lf/k89;

    .line 56
    .line 57
    invoke-virtual {v8, v0}, Lf/x44;->US1(I)Z

    .line 58
    .line 59
    .line 60
    move-result v8

    .line 61
    const/4 v9, 0x3

    .line 62
    const/4 v10, 0x2

    .line 63
    const/4 v11, 0x0

    .line 64
    if-eqz v8, :cond_bc

    .line 65
    .line 66
    iget-object v8, v1, Lf/aw3;->SE0:Lf/k89;

    .line 67
    .line 68
    invoke-virtual {v8, v0}, Lf/k89;->get(I)Ljava/lang/Object;

    .line 69
    .line 70
    .line 71
    move-result-object v0

    .line 72
    check-cast v0, Lf/z46;

    .line 73
    .line 74
    :try_start_49
    iget-object v8, v1, Lf/aw3;->rh1:Lf/z74;

    .line 75
    .line 76
    instance-of v12, v0, Lf/u47;

    .line 77
    .line 78
    if-eqz v12, :cond_6d

    .line 79
    .line 80
    move-object v12, v0

    .line 81
    check-cast v12, Lf/u47;

    .line 82
    .line 83
    iget-object v13, v12, Lf/u47;->UN1:Ljava/util/zip/ZipEntry;

    .line 84
    .line 85
    if-nez v13, :cond_57

    .line 86
    .line 87
    goto :goto_68

    .line 88
    :cond_57
    invoke-virtual {v13}, Ljava/util/zip/ZipEntry;->getMethod()I

    .line 89
    .line 90
    .line 91
    move-result v13

    .line 92
    if-eqz v13, :cond_68

    .line 93
    .line 94
    sget-object v12, Lf/dq7;->Et:Lf/nx3;

    .line 95
    .line 96
    new-instance v13, Lf/ni3;

    .line 97
    .line 98
    invoke-direct {v13, v7}, Lf/ni3;-><init>(I)V

    .line 99
    .line 100
    .line 101
    invoke-virtual {v12, v13}, Lf/nx3;->d3(Ljava/lang/Runnable;)V

    .line 102
    .line 103
    .line 104
    goto :goto_6d

    .line 105
    :cond_68
    :goto_68
    new-instance v0, Lf/un5;

    .line 106
    .line 107
    invoke-direct {v0, v12}, Lf/un5;-><init>(Lf/u47;)V

    .line 108
    .line 109
    .line 110
    :cond_6d
    :goto_6d
    new-instance v12, Lf/w71;

    .line 111
    .line 112
    iget-object v8, v8, Lf/z74;->MG0:Ljava/util/List;

    .line 113
    .line 114
    invoke-direct {v12, v0, v8}, Lf/w71;-><init>(Lf/z46;Ljava/util/List;)V

    .line 115
    .line 116
    .line 117
    iget-boolean v0, v12, Lf/kn7;->ZY0:Z

    .line 118
    .line 119
    if-nez v0, :cond_99

    .line 120
    .line 121
    iput-byte v2, v12, Lf/w71;->EM:B

    .line 122
    .line 123
    iput-short v3, v12, Lf/w71;->WV0:S

    .line 124
    .line 125
    sget-object v0, Lf/fu0;->fV1:Lf/fu0;

    .line 126
    .line 127
    if-ne v4, v0, :cond_82

    .line 128
    .line 129
    const/4 v0, 0x1

    .line 130
    goto :goto_83

    .line 131
    :cond_82
    const/4 v0, 0x0

    .line 132
    :goto_83
    iput-boolean v0, v12, Lf/w71;->xa:Z

    .line 133
    .line 134
    iget-boolean v8, v12, Lf/kn7;->ZY0:Z

    .line 135
    .line 136
    if-nez v8, :cond_93

    .line 137
    .line 138
    iget-object v8, v12, Lf/w71;->m90:Lf/yh3;

    .line 139
    .line 140
    iget-object v8, v8, Lf/yh3;->nM0:Landroid/media/MediaPlayer;

    .line 141
    .line 142
    if-nez v8, :cond_90

    .line 143
    .line 144
    goto :goto_93

    .line 145
    :cond_90
    invoke-virtual {v8, v0}, Landroid/media/MediaPlayer;->setLooping(Z)V

    .line 146
    .line 147
    .line 148
    :cond_93
    :goto_93
    invoke-virtual {v12, v6}, Lf/w71;->cv(F)V

    .line 149
    .line 150
    .line 151
    return-object v12

    .line 152
    :catch_97
    move-exception v0

    .line 153
    goto :goto_a7

    .line 154
    :cond_99
    sget-object v0, Lf/aw3;->pC1:Lf/xv7;

    .line 155
    .line 156
    invoke-static {v2}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 157
    .line 158
    .line 159
    move-result-object v8

    .line 160
    invoke-static {v3}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 161
    .line 162
    .line 163
    move-result-object v12

    .line 164
    invoke-interface {v0, v5, v8, v12}, Lf/xv7;->error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V
    :try_end_a6
    .catch Ljava/lang/Exception; {:try_start_49 .. :try_end_a6} :catch_97

    .line 165
    .line 166
    .line 167
    goto :goto_bc

    .line 168
    :goto_a7
    sget-object v8, Lf/aw3;->pC1:Lf/xv7;

    .line 169
    .line 170
    invoke-static {v2}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 171
    .line 172
    .line 173
    move-result-object v12

    .line 174
    invoke-static {v3}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 175
    .line 176
    .line 177
    move-result-object v13

    .line 178
    new-array v14, v9, [Ljava/lang/Object;

    .line 179
    .line 180
    aput-object v12, v14, v11

    .line 181
    .line 182
    aput-object v13, v14, v7

    .line 183
    .line 184
    aput-object v0, v14, v10

    .line 185
    .line 186
    invoke-interface {v8, v5, v14}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 187
    .line 188
    .line 189
    :cond_bc
    :goto_bc
    invoke-static {v2}, Lf/o80;->eF1(B)Z

    .line 190
    .line 191
    .line 192
    move-result v0

    .line 193
    if-eqz v0, :cond_101

    .line 194
    .line 195
    sget-object v0, Lf/p37;->T10:Lf/zw0;

    .line 196
    .line 197
    invoke-virtual {v0, v2}, Lf/zw0;->Ux(B)Lf/sg7;

    .line 198
    .line 199
    .line 200
    move-result-object v0

    .line 201
    if-eqz v0, :cond_101

    .line 202
    .line 203
    if-ne v2, v10, :cond_d6

    .line 204
    .line 205
    const/16 v0, 0x547

    .line 206
    .line 207
    if-ne v3, v0, :cond_d6

    .line 208
    .line 209
    new-instance v0, Lf/w47;

    .line 210
    .line 211
    invoke-direct {v0, v2, v3}, Lf/w47;-><init>(BS)V

    .line 212
    .line 213
    .line 214
    return-object v0

    .line 215
    :cond_d6
    sget-object v0, Lf/p37;->T10:Lf/zw0;

    .line 216
    .line 217
    invoke-virtual {v0, v2}, Lf/zw0;->Ux(B)Lf/sg7;

    .line 218
    .line 219
    .line 220
    move-result-object v0

    .line 221
    invoke-virtual {v0}, Lf/sg7;->Tc1()Lf/fn8;

    .line 222
    .line 223
    .line 224
    move-result-object v0

    .line 225
    iget-object v0, v0, Lf/fn8;->O3:Lf/gc2;

    .line 226
    .line 227
    iget-object v0, v0, Lf/gc2;->sz:[Ljava/lang/Object;

    .line 228
    .line 229
    check-cast v0, [Lf/oi1;

    .line 230
    .line 231
    aget-object v0, v0, v11

    .line 232
    .line 233
    iget-object v0, v0, Lf/oi1;->mU:Ljava/lang/Object;

    .line 234
    .line 235
    check-cast v0, [Lf/m89;

    .line 236
    .line 237
    array-length v5, v0

    .line 238
    if-lt v3, v5, :cond_f0

    .line 239
    .line 240
    goto :goto_fb

    .line 241
    :cond_f0
    aget-object v0, v0, v3

    .line 242
    .line 243
    check-cast v0, Lf/yu0;

    .line 244
    .line 245
    iget-short v0, v0, Lf/yu0;->Dk0:S

    .line 246
    .line 247
    const/16 v5, 0x4e49

    .line 248
    .line 249
    if-eq v0, v5, :cond_fb

    .line 250
    .line 251
    goto :goto_101

    .line 252
    :cond_fb
    :goto_fb
    new-instance v0, Lf/w47;

    .line 253
    .line 254
    invoke-direct {v0, v2, v3}, Lf/w47;-><init>(BS)V

    .line 255
    .line 256
    .line 257
    return-object v0

    .line 258
    :cond_101
    :goto_101
    if-eqz v2, :cond_110

    .line 259
    .line 260
    if-eq v2, v7, :cond_106

    .line 261
    .line 262
    goto :goto_11a

    .line 263
    :cond_106
    const/16 v0, 0x261

    .line 264
    .line 265
    if-le v3, v0, :cond_11a

    .line 266
    .line 267
    new-instance v0, Lf/w47;

    .line 268
    .line 269
    invoke-direct {v0, v2, v3}, Lf/w47;-><init>(BS)V

    .line 270
    .line 271
    .line 272
    return-object v0

    .line 273
    :cond_110
    const/16 v0, 0x15a

    .line 274
    .line 275
    if-le v3, v0, :cond_11a

    .line 276
    .line 277
    new-instance v0, Lf/w47;

    .line 278
    .line 279
    invoke-direct {v0, v2, v3}, Lf/w47;-><init>(BS)V

    .line 280
    .line 281
    .line 282
    return-object v0

    .line 283
    :cond_11a
    :goto_11a
    sget-boolean v0, Lf/km0;->Es:Z

    .line 284
    .line 285
    const/4 v5, 0x0

    .line 286
    if-nez v0, :cond_120

    .line 287
    .line 288
    goto :goto_17f

    .line 289
    :cond_120
    sget-object v0, Lf/km0;->q20:Lf/km0;

    .line 290
    .line 291
    if-nez v0, :cond_127

    .line 292
    .line 293
    invoke-static {}, Lf/km0;->init()V

    .line 294
    .line 295
    .line 296
    :cond_127
    :try_start_127
    invoke-static {v2}, Lf/o80;->eF1(B)Z

    .line 297
    .line 298
    .line 299
    move-result v0

    .line 300
    if-eqz v0, :cond_138

    .line 301
    .line 302
    new-instance v0, Lf/q66;

    .line 303
    .line 304
    move/from16 v8, p3

    .line 305
    .line 306
    invoke-direct {v0, v2, v3, v8, v4}, Lf/q66;-><init>(BSSLf/fu0;)V

    .line 307
    .line 308
    .line 309
    :goto_134
    move-object v5, v0

    .line 310
    goto :goto_17f

    .line 311
    :catch_136
    move-exception v0

    .line 312
    goto :goto_166

    .line 313
    :cond_138
    invoke-static {v2}, Lf/o80;->DU(B)Z

    .line 314
    .line 315
    .line 316
    move-result v0

    .line 317
    if-eqz v0, :cond_17f

    .line 318
    .line 319
    new-instance v0, Lf/yu3;

    .line 320
    .line 321
    invoke-direct {v0, v2, v3, v4}, Lf/an;-><init>(BSLf/fu0;)V

    .line 322
    .line 323
    .line 324
    invoke-static {}, Lcom/pokeemu/agbplayj/Agbplayj;->newPlayer()J

    .line 325
    .line 326
    .line 327
    move-result-wide v12

    .line 328
    iput-wide v12, v0, Lf/an;->wt0:J

    .line 329
    .line 330
    const-wide/16 v14, 0x0

    .line 331
    .line 332
    cmp-long v4, v12, v14

    .line 333
    .line 334
    if-eqz v4, :cond_15e

    .line 335
    .line 336
    invoke-static {v12, v13, v2, v3}, Lcom/pokeemu/agbplayj/Agbplayj;->loadSong(JBI)I

    .line 337
    .line 338
    .line 339
    move-result v4

    .line 340
    if-eqz v4, :cond_156

    .line 341
    .line 342
    goto :goto_134

    .line 343
    :cond_156
    new-instance v0, Ljava/lang/RuntimeException;

    .line 344
    .line 345
    const-string v4, "Unable to load agbplayj"

    .line 346
    .line 347
    invoke-direct {v0, v4}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    .line 348
    .line 349
    .line 350
    throw v0

    .line 351
    :cond_15e
    new-instance v0, Ljava/lang/RuntimeException;

    .line 352
    .line 353
    const-string v4, "Unable to allocate player"

    .line 354
    .line 355
    invoke-direct {v0, v4}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    .line 356
    .line 357
    .line 358
    throw v0
    :try_end_166
    .catch Ljava/lang/Exception; {:try_start_127 .. :try_end_166} :catch_136

    .line 359
    :goto_166
    sget-object v4, Lf/km0;->xn1:Lf/xv7;

    .line 360
    .line 361
    invoke-static {v2}, Ljava/lang/Byte;->valueOf(B)Ljava/lang/Byte;

    .line 362
    .line 363
    .line 364
    move-result-object v8

    .line 365
    invoke-static {v3}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 366
    .line 367
    .line 368
    move-result-object v12

    .line 369
    new-array v9, v9, [Ljava/lang/Object;

    .line 370
    .line 371
    aput-object v8, v9, v11

    .line 372
    .line 373
    aput-object v12, v9, v7

    .line 374
    .line 375
    aput-object v0, v9, v10

    .line 376
    .line 377
    const-string v0, "NativeSoundPlayer[{},{}] Initialize error"

    .line 378
    .line 379
    invoke-interface {v4, v0, v9}, Lf/xv7;->error(Ljava/lang/String;[Ljava/lang/Object;)V

    .line 380
    .line 381
    .line 382
    # MonMMO-EX: retail switches the whole native (ROM) sound engine off here - `km0.Es = false` -
    # the first time ONE song fails to load, and nothing ever turns it back on. A follower or party
    # member from the Expansion has no cry in the Black ROM's SDAT (cries are `loadSSEQ(2, 1, species)`),
    # so its first cry after login threw "Unable to load sseq" and every map song after it was silent.
    # The failed sound already returns the silent player below; the engine stays up for the rest.
    # (was: sput-boolean v11, Lf/km0;->Es:Z)

    .line 383
    .line 384
    :cond_17f
    :goto_17f
    if-eqz v5, :cond_185

    .line 385
    .line 386
    invoke-virtual {v5, v6}, Lf/an;->cv(F)V

    .line 387
    .line 388
    .line 389
    return-object v5

    .line 390
    :cond_185
    new-instance v0, Lf/w47;

    .line 391
    .line 392
    invoke-direct {v0, v2, v3}, Lf/w47;-><init>(BS)V

    .line 393
    .line 394
    .line 395
    return-object v0

    .line 396
    :cond_18b
    :goto_18b
    new-instance v0, Lf/w47;

    .line 397
    .line 398
    invoke-direct {v0, v2, v3}, Lf/w47;-><init>(BS)V

    .line 399
    .line 400
    .line 401
    return-object v0
.end method

.method public final Gz(BSSZFFFI)V
    .registers 20

    .line 1
    sget-object v0, Lf/f72;->Bt0:Lf/f72;

    .line 2
    .line 3
    new-instance v1, Lf/j29;

    .line 4
    .line 5
    const/4 v10, 0x1

    .line 6
    move-object v2, p0

    .line 7
    move v5, p1

    .line 8
    move v6, p2

    .line 9
    move v7, p3

    .line 10
    move v4, p4

    .line 11
    move/from16 v8, p5

    .line 12
    .line 13
    move/from16 v9, p6

    .line 14
    .line 15
    move/from16 v3, p7

    .line 16
    .line 17
    invoke-direct/range {v1 .. v10}, Lf/j29;-><init>(Lf/aw3;FZBSSFFI)V

    .line 18
    .line 19
    .line 20
    move/from16 p1, p8

    .line 21
    .line 22
    int-to-long p1, p1

    .line 23
    invoke-virtual {v0, v1, p1, p2}, Lf/f72;->rY0(Ljava/lang/Runnable;J)V

    .line 24
    .line 25
    .line 26
    return-void
.end method

.method public final JV(S)V
    .registers 5

    .line 1
    sget-object v0, Lf/dq7;->Et:Lf/nx3;

    .line 2
    .line 3
    new-instance v1, Lf/g86;

    .line 4
    .line 5
    const/16 v2, 0x9

    .line 6
    .line 7
    invoke-direct {v1, p0, p1, v2}, Lf/g86;-><init>(Ljava/lang/Object;SI)V

    .line 8
    .line 9
    .line 10
    invoke-virtual {v0, v1}, Lf/nx3;->d3(Ljava/lang/Runnable;)V

    .line 11
    .line 12
    .line 13
    return-void
.end method

.method public final Sv(Z)V
    .registers 5

    .line 1
    sget-object v0, Lf/dq7;->Et:Lf/nx3;

    .line 2
    .line 3
    new-instance v1, Lf/kk;

    .line 4
    .line 5
    const/4 v2, 0x0

    .line 6
    invoke-direct {v1, p0, p1, v2}, Lf/kk;-><init>(Lf/aw3;ZI)V

    .line 7
    .line 8
    .line 9
    invoke-virtual {v0, v1}, Lf/nx3;->d3(Ljava/lang/Runnable;)V

    .line 10
    .line 11
    .line 12
    return-void
.end method

.method public final i4(BS)V
    .registers 6

    .line 1
    sget-object v0, Lf/dq7;->Et:Lf/nx3;

    .line 2
    .line 3
    new-instance v1, Lf/a11;

    .line 4
    .line 5
    const/4 v2, 0x0

    .line 6
    invoke-direct {v1, p0, p1, p2, v2}, Lf/a11;-><init>(Ljava/lang/Object;BSI)V

    .line 7
    .line 8
    .line 9
    invoke-virtual {v0, v1}, Lf/nx3;->d3(Ljava/lang/Runnable;)V

    .line 10
    .line 11
    .line 12
    return-void
.end method

.method public final ia()V
    .registers 3

    .line 1
    new-instance v0, Lf/bj0;

    .line 2
    .line 3
    const/16 v1, 0xd

    .line 4
    .line 5
    invoke-direct {v0, v1}, Lf/bj0;-><init>(I)V

    .line 6
    .line 7
    .line 8
    iget-object v1, p0, Lf/aw3;->AZ:Ljava/util/ArrayList;

    .line 9
    .line 10
    invoke-static {v1, v0}, Lj$/util/Collection$-EL;->removeIf(Ljava/util/Collection;Ljava/util/function/Predicate;)Z

    .line 11
    .line 12
    .line 13
    return-void
.end method

.method public final o21(BSZZ)V
    .registers 12

    .line 1
    sget-object v0, Lf/km0;->q20:Lf/km0;

    .line 2
    .line 3
    if-eqz v0, :cond_1a

    .line 4
    .line 5
    invoke-virtual {v0}, Ljava/lang/Thread;->isAlive()Z

    .line 6
    .line 7
    .line 8
    move-result v0

    .line 9
    if-nez v0, :cond_b

    .line 10
    .line 11
    goto :goto_1a

    .line 12
    :cond_b
    sget-object v0, Lf/dq7;->Et:Lf/nx3;

    .line 13
    .line 14
    new-instance v1, Lf/y53;

    .line 15
    .line 16
    move-object v2, p0

    .line 17
    move v3, p1

    .line 18
    move v4, p2

    .line 19
    move v5, p3

    .line 20
    move v6, p4

    .line 21
    invoke-direct/range {v1 .. v6}, Lf/y53;-><init>(Lf/aw3;BSZZ)V

    .line 22
    .line 23
    .line 24
    invoke-virtual {v0, v1}, Lf/nx3;->d3(Ljava/lang/Runnable;)V

    .line 25
    .line 26
    .line 27
    :cond_1a
    :goto_1a
    return-void
.end method

.method public final ob1(BSZ)V
    .registers 11

    .line 1
    sget-object v0, Lf/dq7;->Et:Lf/nx3;

    .line 2
    .line 3
    new-instance v1, Lf/qn6;

    .line 4
    .line 5
    const/4 v6, 0x0

    .line 6
    move-object v2, p0

    .line 7
    move v4, p1

    .line 8
    move v5, p2

    .line 9
    move v3, p3

    .line 10
    invoke-direct/range {v1 .. v6}, Lf/qn6;-><init>(Lf/aw3;ZBSF)V

    .line 11
    .line 12
    .line 13
    invoke-virtual {v0, v1}, Lf/nx3;->d3(Ljava/lang/Runnable;)V

    .line 14
    .line 15
    .line 16
    return-void
.end method

.method public final vI1(BSZFFI)V
    .registers 16

    .line 1
    const/4 v3, -0x1

    .line 2
    const/high16 v6, 0x3f800000    # 1.0f

    .line 3
    .line 4
    move-object v0, p0

    .line 5
    move v1, p1

    .line 6
    move v2, p2

    .line 7
    move v4, p3

    .line 8
    move v5, p4

    .line 9
    move v7, p5

    .line 10
    move v8, p6

    .line 11
    invoke-virtual/range {v0 .. v8}, Lf/aw3;->Gz(BSSZFFFI)V

    .line 12
    .line 13
    .line 14
    return-void
.end method

.method public final yz(BS)V
    .registers 5

    .line 1
    const/4 v0, 0x1

    .line 2
    const/4 v1, 0x0

    .line 3
    invoke-virtual {p0, p1, p2, v0, v1}, Lf/aw3;->o21(BSZZ)V

    .line 4
    .line 5
    .line 6
    return-void
.end method
