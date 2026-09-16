.class public Lf/dw2;
.super Lf/ni6;


# static fields
.field public static final qW1:[Lf/c67;


# instance fields
.field public final Br0:B

.field public Bu:Z

.field public Fs0:B

.field public final JK:B

.field public Lq0:Lf/br8;

.field public final Sq:B

.field public UR0:Lf/cc6;

.field public final Wb:Lf/kn5;

.field public YM0:[S

.field public Yl0:Lf/uq3;

.field public final Zk0:Z

.field public final b11:Z

.field public cp:S

.field public final dm0:B

.field public gg0:J

.field public h91:J

.field public iw:Lf/kn5;

.field public final jU0:S

.field public jk:I

.field public kP0:Z

.field public n3:Z

.field public nu1:[B

.field public final q3:B

.field public final ra:Lf/jd4;

.field public ty1:B

.field public final vg:B

.field public x71:B

.field public xU1:F

.field public y9:S

.field public zs1:Z


# direct methods
.method static constructor <clinit>()V
    .registers 3

    .line 1
    const/4 v0, 0x4

    .line 2
    new-array v0, v0, [Lf/c67;

    .line 3
    .line 4
    sget-object v1, Lf/c67;->hZ1:Lf/c67;

    .line 5
    .line 6
    const/4 v2, 0x0

    .line 7
    aput-object v1, v0, v2

    .line 8
    .line 9
    sget-object v1, Lf/c67;->by0:Lf/c67;

    .line 10
    .line 11
    const/4 v2, 0x1

    .line 12
    aput-object v1, v0, v2

    .line 13
    .line 14
    sget-object v1, Lf/c67;->q01:Lf/c67;

    .line 15
    .line 16
    const/4 v2, 0x2

    .line 17
    aput-object v1, v0, v2

    .line 18
    .line 19
    sget-object v1, Lf/c67;->AR1:Lf/c67;

    .line 20
    .line 21
    const/4 v2, 0x3

    .line 22
    aput-object v1, v0, v2

    .line 23
    .line 24
    sput-object v0, Lf/dw2;->qW1:[Lf/c67;

    .line 25
    .line 26
    return-void
.end method

.method public constructor <init>(Lf/nw0;BSBBBBBBLf/jd4;SBZZLf/uq3;[S[B)V
    .registers 22

    move/from16 v0, p12

    const/4 v1, 0x0

    invoke-direct {p0, p1, p10, v1}, Lf/ni6;-><init>(Lf/nw0;Lf/jd4;B)V

    iput v1, p0, Lf/dw2;->jk:I

    const-wide/16 v2, -0x1

    iput-wide v2, p0, Lf/dw2;->h91:J

    const-wide/16 v2, 0x0

    iput-wide v2, p0, Lf/dw2;->gg0:J

    iput-boolean v1, p0, Lf/dw2;->kP0:Z

    iput-boolean v1, p0, Lf/dw2;->Bu:Z

    const/high16 p1, 0x3f800000    # 1.0f

    iput p1, p0, Lf/dw2;->xU1:F

    iput-short v1, p0, Lf/dw2;->cp:S

    iput-byte v1, p0, Lf/dw2;->x71:B

    iput-byte p2, p0, Lf/dw2;->Fs0:B

    iput-short p3, p0, Lf/dw2;->y9:S

    .line 1
    sget-object p1, Lf/f2;->rt1:Lf/f2;

    .line 2
    invoke-virtual {p1, p2, p3, v1}, Lf/f2;->bh0(BIZ)Lf/cc6;

    move-result-object p1

    .line 3
    iput-object p1, p0, Lf/dw2;->UR0:Lf/cc6;

    const/4 p1, 0x1

    if-ne p2, p1, :cond_52

    const/16 p1, 0x3b

    if-eq p3, p1, :cond_48

    const/16 p1, 0x52

    if-eq p3, p1, :cond_48

    const/16 p1, 0x61

    if-eq p3, p1, :cond_48

    const/16 p1, 0x72

    if-eq p3, p1, :cond_48

    const/16 p1, 0xe4

    if-eq p3, p1, :cond_48

    const/16 p1, 0x56

    if-eq p3, p1, :cond_48

    const/16 p1, 0x57

    if-eq p3, p1, :cond_48

    goto :goto_49

    :cond_48
    const/4 p5, 0x0

    :goto_49
    const/16 p1, 0x8e

    if-lt p3, p1, :cond_52

    const/16 p1, 0xbc

    if-gt p3, p1, :cond_52

    const/4 p5, 0x0

    :cond_52
    iput-byte p4, p0, Lf/dw2;->dm0:B

    .line 4
    sget-object p1, Lf/j96;->oi1:Lf/j96;

    .line 5
    iget-byte p2, p10, Lf/jd4;->oD0:B

    .line 6
    iget-object p1, p1, Lf/j96;->cOM4:[Lf/k33;

    .line 7
    aget-object p1, p1, p2

    invoke-virtual {p1, p5}, Lf/k33;->VK0(B)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Lf/kn5;

    invoke-virtual {p1, p4}, Lf/kn5;->c40(B)Lf/kn5;

    move-result-object p1

    .line 8
    iput-object p1, p0, Lf/dw2;->iw:Lf/kn5;

    iput-object p1, p0, Lf/dw2;->Wb:Lf/kn5;

    iput-byte p6, p0, Lf/dw2;->JK:B

    iput-byte p7, p0, Lf/dw2;->Sq:B

    iput-byte p8, p0, Lf/dw2;->vg:B

    iput-byte p9, p0, Lf/dw2;->Br0:B

    move p1, p11

    iput-short p1, p0, Lf/dw2;->jU0:S

    iput-byte v0, p0, Lf/dw2;->q3:B

    iput-byte v0, p0, Lf/dw2;->ty1:B

    move/from16 p1, p13

    iput-boolean p1, p0, Lf/dw2;->b11:Z

    move/from16 p1, p14

    iput-boolean p1, p0, Lf/dw2;->Zk0:Z

    move-object/from16 p1, p15

    iput-object p1, p0, Lf/dw2;->Yl0:Lf/uq3;

    move-object/from16 p1, p16

    iput-object p1, p0, Lf/dw2;->YM0:[S

    move-object/from16 p1, p17

    iput-object p1, p0, Lf/dw2;->nu1:[B

    invoke-virtual {p10}, Lf/jd4;->Wu()Lf/jd4;

    move-result-object p1

    iput-object p1, p0, Lf/dw2;->ra:Lf/jd4;

    .line 9
    iget-object p1, p0, Lf/ni6;->pQ1:Lf/rp3;

    .line 10
    invoke-virtual {p1}, Lf/rp3;->PL0()V

    invoke-virtual {p0}, Lf/dw2;->Kk0()V

    return-void
.end method


# virtual methods
.method public final Bl()Ljava/lang/String;
    .registers 3

    .line 1
    new-instance v0, Ljava/lang/StringBuilder;

    .line 2
    .line 3
    const-string v1, "Npc"

    .line 4
    .line 5
    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 6
    .line 7
    .line 8
    iget-object v1, p0, Lf/t78;->iE:Ljava/lang/Object;

    .line 9
    .line 10
    check-cast v1, Lf/nw0;

    .line 11
    .line 12
    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    .line 13
    .line 14
    .line 15
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    .line 16
    .line 17
    .line 18
    move-result-object v0

    .line 19
    return-object v0
.end method

.method public C80()Lf/rp3;
    .registers 2

    .line 1
    new-instance v0, Lf/fu;

    .line 2
    .line 3
    invoke-direct {v0, p0}, Lf/fu;-><init>(Lf/dw2;)V

    .line 4
    .line 5
    .line 6
    return-object v0
.end method

.method public final Eu()Z
    .registers 2

    .line 1
    const/4 v0, 0x1

    .line 2
    return v0
.end method

.method public final Hm1(B)V
    .registers 8

    .line 1
    sget-object v0, Lf/j96;->oi1:Lf/j96;

    .line 2
    .line 3
    iget-object v1, p0, Lf/ni6;->YO:Lf/jd4;

    .line 4
    .line 5
    iget-byte v2, v1, Lf/jd4;->oD0:B

    .line 6
    .line 7
    iget-object v0, v0, Lf/j96;->cOM4:[Lf/k33;

    .line 8
    .line 9
    aget-object v0, v0, v2

    .line 10
    .line 11
    invoke-virtual {v0, p1}, Lf/k33;->VK0(B)Ljava/lang/Object;

    .line 12
    .line 13
    .line 14
    move-result-object p1

    .line 15
    check-cast p1, Lf/kn5;

    .line 16
    .line 17
    iget-byte v0, p0, Lf/dw2;->dm0:B

    .line 18
    .line 19
    invoke-virtual {p1, v0}, Lf/kn5;->c40(B)Lf/kn5;

    .line 20
    .line 21
    .line 22
    move-result-object p1

    .line 23
    iput-object p1, p0, Lf/dw2;->iw:Lf/kn5;

    .line 24
    .line 25
    iget-object v2, p0, Lf/dw2;->Wb:Lf/kn5;

    .line 26
    .line 27
    const/4 v3, 0x0

    .line 28
    iput v3, p0, Lf/dw2;->jk:I

    .line 29
    .line 30
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    .line 31
    .line 32
    .line 33
    move-result-wide v2

    .line 34
    iget-object p1, p0, Lf/dw2;->iw:Lf/kn5;

    .line 35
    .line 36
    invoke-virtual {p1}, Lf/kn5;->gT0()I

    .line 37
    .line 38
    .line 39
    move-result p1

    .line 40
    int-to-long v4, p1

    .line 41
    add-long/2addr v2, v4

    .line 42
    iput-wide v2, p0, Lf/dw2;->h91:J

    .line 43
    .line 44
    iput-byte v0, v1, Lf/jd4;->KC1:B

    .line 45
    .line 46
    return-void
.end method

.method public final Im()Z
    .registers 3

    .line 1
    iget-short v0, p0, Lf/dw2;->y9:S

    .line 2
    .line 3
    const/16 v1, 0x141

    .line 4
    .line 5
    if-ne v0, v1, :cond_e

    .line 6
    .line 7
    iget-byte v0, p0, Lf/dw2;->Fs0:B

    .line 8
    .line 9
    const/16 v1, 0xa

    .line 10
    .line 11
    if-ne v0, v1, :cond_e

    .line 12
    .line 13
    const/4 v0, 0x1

    .line 14
    return v0

    .line 15
    :cond_e
    invoke-super {p0}, Lf/ni6;->Im()Z

    .line 16
    .line 17
    .line 18
    move-result v0

    .line 19
    return v0
.end method

.method public final Kk0()V
    .registers 8

    .line 1
    iget-byte v0, p0, Lf/dw2;->Fs0:B

    .line 2
    .line 3
    const/16 v1, 0xa

    .line 4
    .line 5
    const/4 v2, 0x0

    .line 6
    if-ne v0, v1, :cond_c

    .line 7
    .line 8
    iput-short v2, p0, Lf/dw2;->cp:S

    .line 9
    .line 10
    iput-byte v2, p0, Lf/dw2;->x71:B

    .line 11
    .line 12
    return-void

    .line 13
    :cond_c
    const/4 v1, 0x4

    .line 14
    if-ne v0, v1, :cond_2d

    .line 15
    .line 16
    sget-object v0, Lf/p37;->T10:Lf/zw0;

    .line 17
    .line 18
    iget-object v0, v0, Lf/zw0;->WP1:Lf/rf3;

    .line 19
    .line 20
    if-eqz v0, :cond_2d

    .line 21
    .line 22
    iget-short v3, p0, Lf/dw2;->y9:S

    .line 23
    .line 24
    iget-object v0, v0, Lf/rf3;->nX0:Lf/ry6;

    .line 25
    .line 26
    invoke-virtual {v0, v3}, Lf/ry6;->Lg(S)S

    .line 27
    .line 28
    .line 29
    move-result v0

    .line 30
    const/16 v3, 0x128

    .line 31
    .line 32
    if-le v0, v3, :cond_2d

    .line 33
    .line 34
    add-int/lit16 v0, v0, 0xed7

    .line 35
    .line 36
    int-to-short v0, v0

    .line 37
    invoke-static {v0}, Lf/o80;->T02(S)S

    .line 38
    .line 39
    .line 40
    move-result v0

    .line 41
    iput-short v0, p0, Lf/dw2;->cp:S

    .line 42
    .line 43
    iput-byte v2, p0, Lf/dw2;->x71:B

    .line 44
    .line 45
    return-void

    .line 46
    :cond_2d
    iget-short v0, p0, Lf/dw2;->y9:S

    # MonMMO: graphics id 20000 + N draws the npc with species N's follower sheet (sprite sets 4
    # and 10 are handled above). o80.T02 below only reaches the retail species.
    const/16 v3, 0x4e20

    if-lt v0, v3, :monmmo_retail_follower

    sub-int/2addr v0, v3

    # MonMMO 2026-09-15: 30000 + N is the same sheet one size up - follower flag 0x80, which the
    # follower entity (f/vc1.eL0, fed by d11() = x71) draws at 4/3 scale, as retail does for alphas.
    const/16 v3, 0x2710

    if-lt v0, v3, :monmmo_follower_normal

    sub-int/2addr v0, v3

    int-to-short v0, v0

    iput-short v0, p0, Lf/dw2;->cp:S

    const/16 v3, -0x80

    iput-byte v3, p0, Lf/dw2;->x71:B

    return-void

    :monmmo_follower_normal
    int-to-short v0, v0

    iput-short v0, p0, Lf/dw2;->cp:S

    iput-byte v2, p0, Lf/dw2;->x71:B

    return-void

    :monmmo_retail_follower
    .line 47
    .line 48
    const/16 v3, 0xfff

    .line 49
    .line 50
    if-lt v0, v3, :cond_19e

    .line 51
    .line 52
    const/16 v4, 0x1f9e

    .line 53
    .line 54
    if-le v0, v4, :cond_39

    .line 55
    .line 56
    goto/16 :goto_19e

    .line 57
    .line 58
    :cond_39
    invoke-static {v0}, Lf/o80;->T02(S)S

    .line 59
    .line 60
    .line 61
    move-result v0

    .line 62
    iput-short v0, p0, Lf/dw2;->cp:S

    .line 63
    .line 64
    iget-short v0, p0, Lf/dw2;->y9:S

    .line 65
    .line 66
    const/16 v5, 0x17cf

    .line 67
    .line 68
    if-lt v0, v5, :cond_4c

    .line 69
    .line 70
    const/16 v5, 0x1bb6

    .line 71
    .line 72
    if-gt v0, v5, :cond_4c

    .line 73
    .line 74
    const/16 v5, 0x40

    .line 75
    .line 76
    goto :goto_4d

    .line 77
    :cond_4c
    const/4 v5, 0x0

    .line 78
    :goto_4d
    const/16 v6, 0x1bb7

    .line 79
    .line 80
    if-lt v0, v6, :cond_56

    .line 81
    .line 82
    if-gt v0, v4, :cond_56

    .line 83
    .line 84
    or-int/lit8 v4, v5, -0x80

    .line 85
    .line 86
    int-to-byte v5, v4

    .line 87
    :cond_56
    :goto_56
    const/16 v4, 0x13e6

    .line 88
    .line 89
    if-le v0, v4, :cond_5e

    .line 90
    .line 91
    add-int/lit16 v0, v0, -0x3e8

    .line 92
    .line 93
    int-to-short v0, v0

    .line 94
    goto :goto_56

    .line 95
    :cond_5e
    if-ge v0, v3, :cond_62

    .line 96
    .line 97
    goto/16 :goto_196

    .line 98
    .line 99
    :cond_62
    sub-int/2addr v0, v3

    .line 100
    int-to-short v0, v0

    .line 101
    const/4 v3, 0x3

    .line 102
    if-ge v0, v3, :cond_69

    .line 103
    .line 104
    goto/16 :goto_196

    .line 105
    .line 106
    :cond_69
    if-gt v0, v1, :cond_6f

    .line 107
    .line 108
    :cond_6b
    :goto_6b
    sub-int/2addr v0, v3

    .line 109
    :goto_6c
    int-to-byte v2, v0

    .line 110
    goto/16 :goto_196

    .line 111
    .line 112
    :cond_6f
    const/4 v1, 0x1

    .line 113
    sub-int/2addr v0, v1

    .line 114
    int-to-short v0, v0

    .line 115
    const/16 v4, 0x19

    .line 116
    .line 117
    if-ge v0, v4, :cond_78

    .line 118
    .line 119
    goto/16 :goto_196

    .line 120
    .line 121
    :cond_78
    const/16 v6, 0x1a

    .line 122
    .line 123
    if-gt v0, v6, :cond_7e

    .line 124
    .line 125
    :cond_7c
    :goto_7c
    sub-int/2addr v0, v4

    .line 126
    goto :goto_6c

    .line 127
    :cond_7e
    sub-int/2addr v0, v1

    .line 128
    int-to-short v0, v0

    .line 129
    const/16 v4, 0x9a

    .line 130
    .line 131
    if-ge v0, v4, :cond_86

    .line 132
    .line 133
    goto/16 :goto_196

    .line 134
    .line 135
    :cond_86
    const/16 v6, 0x9b

    .line 136
    .line 137
    if-gt v0, v6, :cond_8b

    .line 138
    .line 139
    goto :goto_7c

    .line 140
    :cond_8b
    sub-int/2addr v0, v1

    .line 141
    int-to-short v0, v0

    .line 142
    const/16 v4, 0xac

    .line 143
    .line 144
    if-ge v0, v4, :cond_93

    .line 145
    .line 146
    goto/16 :goto_196

    .line 147
    .line 148
    :cond_93
    const/16 v6, 0xad

    .line 149
    .line 150
    if-gt v0, v6, :cond_98

    .line 151
    .line 152
    goto :goto_7c

    .line 153
    :cond_98
    sub-int/2addr v0, v1

    .line 154
    int-to-short v0, v0

    .line 155
    const/16 v4, 0xc9

    .line 156
    .line 157
    if-ge v0, v4, :cond_a0

    .line 158
    .line 159
    goto/16 :goto_196

    .line 160
    .line 161
    :cond_a0
    const/16 v6, 0xe4

    .line 162
    .line 163
    if-gt v0, v6, :cond_a5

    .line 164
    .line 165
    goto :goto_7c

    .line 166
    :cond_a5
    add-int/lit8 v0, v0, -0x1b

    .line 167
    .line 168
    int-to-short v0, v0

    .line 169
    const/16 v4, 0xcb

    .line 170
    .line 171
    if-gt v0, v4, :cond_af

    .line 172
    .line 173
    add-int/lit16 v0, v0, -0xca

    .line 174
    .line 175
    goto :goto_6c

    .line 176
    :cond_af
    sub-int/2addr v0, v1

    .line 177
    int-to-short v0, v0

    .line 178
    const/16 v4, 0xd0

    .line 179
    .line 180
    if-ge v0, v4, :cond_b7

    .line 181
    .line 182
    goto/16 :goto_196

    .line 183
    .line 184
    :cond_b7
    const/16 v6, 0xd1

    .line 185
    .line 186
    if-gt v0, v6, :cond_bc

    .line 187
    .line 188
    goto :goto_7c

    .line 189
    :cond_bc
    sub-int/2addr v0, v1

    .line 190
    int-to-short v0, v0

    .line 191
    const/16 v4, 0xd6

    .line 192
    .line 193
    if-ge v0, v4, :cond_c4

    .line 194
    .line 195
    goto/16 :goto_196

    .line 196
    .line 197
    :cond_c4
    const/16 v6, 0xd7

    .line 198
    .line 199
    if-gt v0, v6, :cond_c9

    .line 200
    .line 201
    goto :goto_7c

    .line 202
    :cond_c9
    sub-int/2addr v0, v1

    .line 203
    int-to-short v0, v0

    .line 204
    const/16 v4, 0x182

    .line 205
    .line 206
    if-ge v0, v4, :cond_d1

    .line 207
    .line 208
    goto/16 :goto_196

    .line 209
    .line 210
    :cond_d1
    const/16 v6, 0x184

    .line 211
    .line 212
    if-gt v0, v6, :cond_d6

    .line 213
    .line 214
    goto :goto_7c

    .line 215
    :cond_d6
    sub-int/2addr v0, v3

    .line 216
    int-to-short v0, v0

    .line 217
    const/16 v3, 0x19c

    .line 218
    .line 219
    if-ge v0, v3, :cond_de

    .line 220
    .line 221
    goto/16 :goto_196

    .line 222
    .line 223
    :cond_de
    const/16 v4, 0x19e

    .line 224
    .line 225
    if-gt v0, v4, :cond_e3

    .line 226
    .line 227
    goto :goto_6b

    .line 228
    :cond_e3
    add-int/lit8 v0, v0, -0x2

    .line 229
    .line 230
    int-to-short v0, v0

    .line 231
    const/16 v3, 0x19f

    .line 232
    .line 233
    if-gt v0, v3, :cond_ee

    .line 234
    .line 235
    add-int/lit16 v0, v0, -0x19d

    .line 236
    .line 237
    goto/16 :goto_6c

    .line 238
    .line 239
    :cond_ee
    add-int/lit8 v0, v0, -0x2

    .line 240
    .line 241
    int-to-short v0, v0

    .line 242
    if-ge v0, v3, :cond_f5

    .line 243
    .line 244
    goto/16 :goto_196

    .line 245
    .line 246
    :cond_f5
    const/16 v4, 0x1a0

    .line 247
    .line 248
    if-gt v0, v4, :cond_fb

    .line 249
    .line 250
    goto/16 :goto_6b

    .line 251
    .line 252
    :cond_fb
    sub-int/2addr v0, v1

    .line 253
    int-to-short v0, v0

    .line 254
    const/16 v3, 0x1a6

    .line 255
    .line 256
    if-ge v0, v3, :cond_103

    .line 257
    .line 258
    goto/16 :goto_196

    .line 259
    .line 260
    :cond_103
    const/16 v4, 0x1a7

    .line 261
    .line 262
    if-gt v0, v4, :cond_109

    .line 263
    .line 264
    goto/16 :goto_6b

    .line 265
    .line 266
    :cond_109
    sub-int/2addr v0, v1

    .line 267
    int-to-short v0, v0

    .line 268
    const/16 v3, 0x1a8

    .line 269
    .line 270
    if-gt v0, v3, :cond_111

    .line 271
    .line 272
    goto/16 :goto_7c

    .line 273
    .line 274
    :cond_111
    sub-int/2addr v0, v1

    .line 275
    int-to-short v0, v0

    .line 276
    const/16 v3, 0x1bb

    .line 277
    .line 278
    if-ge v0, v3, :cond_119

    .line 279
    .line 280
    goto/16 :goto_196

    .line 281
    .line 282
    :cond_119
    const/16 v4, 0x1bc

    .line 283
    .line 284
    if-gt v0, v4, :cond_11f

    .line 285
    .line 286
    goto/16 :goto_6b

    .line 287
    .line 288
    :cond_11f
    sub-int/2addr v0, v1

    .line 289
    int-to-short v0, v0

    .line 290
    if-eq v0, v4, :cond_7c

    .line 291
    .line 292
    const/16 v3, 0x1bd

    .line 293
    .line 294
    if-ne v0, v3, :cond_129

    .line 295
    .line 296
    goto/16 :goto_7c

    .line 297
    .line 298
    :cond_129
    sub-int/2addr v0, v1

    .line 299
    int-to-short v0, v0

    .line 300
    if-eq v0, v3, :cond_6b

    .line 301
    .line 302
    const/16 v4, 0x1be

    .line 303
    .line 304
    if-ne v0, v4, :cond_133

    .line 305
    .line 306
    goto/16 :goto_6b

    .line 307
    .line 308
    :cond_133
    sub-int/2addr v0, v1

    .line 309
    int-to-short v0, v0

    .line 310
    const/16 v3, 0x1c1

    .line 311
    .line 312
    if-ge v0, v3, :cond_13a

    .line 313
    .line 314
    goto :goto_196

    .line 315
    :cond_13a
    if-eq v0, v3, :cond_6b

    .line 316
    .line 317
    const/16 v4, 0x1c2

    .line 318
    .line 319
    if-ne v0, v4, :cond_142

    .line 320
    .line 321
    goto/16 :goto_6b

    .line 322
    .line 323
    :cond_142
    sub-int/2addr v0, v1

    .line 324
    int-to-short v0, v0

    .line 325
    if-eq v0, v4, :cond_7c

    .line 326
    .line 327
    const/16 v3, 0x1c3

    .line 328
    .line 329
    if-ne v0, v3, :cond_14c

    .line 330
    .line 331
    goto/16 :goto_7c

    .line 332
    .line 333
    :cond_14c
    sub-int/2addr v0, v1

    .line 334
    int-to-short v0, v0

    .line 335
    const/16 v3, 0x1df

    .line 336
    .line 337
    if-ge v0, v3, :cond_153

    .line 338
    .line 339
    goto :goto_196

    .line 340
    :cond_153
    const/16 v4, 0x1e4

    .line 341
    .line 342
    if-gt v0, v4, :cond_159

    .line 343
    .line 344
    goto/16 :goto_6b

    .line 345
    .line 346
    :cond_159
    add-int/lit8 v0, v0, -0x5

    .line 347
    .line 348
    int-to-short v0, v0

    .line 349
    const/16 v3, 0x1e7

    .line 350
    .line 351
    if-ge v0, v3, :cond_161

    .line 352
    .line 353
    goto :goto_196

    .line 354
    :cond_161
    if-eq v0, v3, :cond_6b

    .line 355
    .line 356
    const/16 v4, 0x1e8

    .line 357
    .line 358
    if-ne v0, v4, :cond_169

    .line 359
    .line 360
    goto/16 :goto_6b

    .line 361
    .line 362
    :cond_169
    sub-int/2addr v0, v1

    .line 363
    int-to-short v0, v0

    .line 364
    const/16 v3, 0x1ec

    .line 365
    .line 366
    if-ge v0, v3, :cond_170

    .line 367
    .line 368
    goto :goto_196

    .line 369
    :cond_170
    if-eq v0, v3, :cond_6b

    .line 370
    .line 371
    const/16 v4, 0x1ed

    .line 372
    .line 373
    if-ne v0, v4, :cond_178

    .line 374
    .line 375
    goto/16 :goto_6b

    .line 376
    .line 377
    :cond_178
    sub-int/2addr v0, v1

    .line 378
    int-to-short v0, v0

    .line 379
    const/16 v3, 0x1ff

    .line 380
    .line 381
    if-gt v0, v3, :cond_180

    .line 382
    .line 383
    goto/16 :goto_7c

    .line 384
    .line 385
    :cond_180
    add-int/lit8 v0, v0, -0x12

    .line 386
    .line 387
    int-to-short v0, v0

    .line 388
    const/16 v3, 0x289

    .line 389
    .line 390
    if-le v0, v3, :cond_18e

    .line 391
    .line 392
    invoke-static {v2, v0}, Lf/o80;->NO(IS)S

    .line 393
    .line 394
    .line 395
    move-result v3

    .line 396
    if-ge v3, v1, :cond_18e

    .line 397
    .line 398
    goto :goto_196

    .line 399
    :cond_18e
    const/16 v1, 0x24c

    .line 400
    .line 401
    if-gt v0, v1, :cond_196

    .line 402
    .line 403
    add-int/lit16 v0, v0, -0x249

    .line 404
    .line 405
    goto/16 :goto_6c

    .line 406
    .line 407
    :cond_196
    :goto_196
    if-ltz v2, :cond_19b

    .line 408
    .line 409
    or-int v0, v5, v2

    .line 410
    .line 411
    int-to-byte v5, v0

    .line 412
    :cond_19b
    iput-byte v5, p0, Lf/dw2;->x71:B

    .line 413
    .line 414
    return-void

    .line 415
    :cond_19e
    :goto_19e
    iput-short v2, p0, Lf/dw2;->cp:S

    .line 416
    .line 417
    iput-byte v2, p0, Lf/dw2;->x71:B

    .line 418
    .line 419
    return-void
.end method

.method public final Og0()Lf/br8;
    .registers 2

    .line 1
    iget-object v0, p0, Lf/dw2;->Lq0:Lf/br8;

    .line 2
    .line 3
    return-object v0
.end method

.method public QX()B
    .registers 3

    .line 1
    iget-boolean v0, p0, Lf/dw2;->kP0:Z

    .line 2
    .line 3
    if-eqz v0, :cond_c

    .line 4
    .line 5
    iget-object v0, p0, Lf/dw2;->UR0:Lf/cc6;

    .line 6
    .line 7
    iget-byte v0, v0, Lf/cc6;->AA:B

    .line 8
    .line 9
    if-eqz v0, :cond_c

    .line 10
    .line 11
    const/4 v0, 0x0

    .line 12
    return v0

    .line 13
    :cond_c
    iget-byte v0, p0, Lf/dw2;->vg:B

    .line 14
    .line 15
    if-ltz v0, :cond_11

    .line 16
    .line 17
    return v0

    .line 18
    :cond_11
    iget-byte v0, p0, Lf/dw2;->Fs0:B

    .line 19
    .line 20
    const/4 v1, 0x2

    .line 21
    if-ne v0, v1, :cond_23

    .line 22
    .line 23
    sget-object v0, Lf/p37;->T10:Lf/zw0;

    .line 24
    .line 25
    iget-object v0, v0, Lf/zw0;->pH1:Lf/v67;

    .line 26
    .line 27
    iget-short v1, p0, Lf/dw2;->y9:S

    .line 28
    .line 29
    invoke-virtual {v0, v1}, Lf/v67;->Ws1(S)Lf/q06;

    .line 30
    .line 31
    .line 32
    move-result-object v0

    .line 33
    iget-byte v0, v0, Lf/q06;->YN0:B

    .line 34
    .line 35
    return v0

    .line 36
    :cond_23
    iget-object v0, p0, Lf/dw2;->UR0:Lf/cc6;

    .line 37
    .line 38
    iget-byte v0, v0, Lf/cc6;->DQ0:B

    .line 39
    .line 40
    return v0
.end method

.method public final co0(Lf/rt0;BLjava/util/ArrayList;)Z
    .registers 16

    .line 1
    sget-object v0, Lf/p37;->Pc0:Lf/li;

    .line 2
    .line 3
    iget-object v1, p0, Lf/ni6;->YO:Lf/jd4;

    .line 4
    .line 5
    iget-byte v2, v1, Lf/jd4;->oD0:B

    .line 6
    .line 7
    iget-byte v3, v1, Lf/jd4;->DS0:B

    .line 8
    .line 9
    iget-byte v4, v1, Lf/jd4;->s:B

    .line 10
    .line 11
    invoke-virtual {v0, v2, v3, v4}, Lf/li;->XL1(BBB)Lf/j23;

    .line 12
    .line 13
    .line 14
    move-result-object v0

    .line 15
    const/4 v2, 0x0

    .line 16
    if-nez v0, :cond_13

    .line 17
    .line 18
    goto/16 :goto_94

    .line 19
    .line 20
    :cond_13
    iget-object p1, p1, Lf/ni6;->YO:Lf/jd4;

    .line 21
    .line 22
    invoke-virtual {p1}, Lf/jd4;->rO()Lf/u28;

    .line 23
    .line 24
    .line 25
    move-result-object p1

    .line 26
    if-nez p1, :cond_1d

    .line 27
    .line 28
    goto/16 :goto_94

    .line 29
    .line 30
    :cond_1d
    invoke-virtual {v1}, Lf/jd4;->rO()Lf/u28;

    .line 31
    .line 32
    .line 33
    move-result-object v3

    .line 34
    if-nez v3, :cond_25

    .line 35
    .line 36
    goto/16 :goto_94

    .line 37
    .line 38
    :cond_25
    move-object v5, v3

    .line 39
    const/4 v3, 0x0

    .line 40
    :goto_27
    iget-byte v4, p0, Lf/dw2;->ty1:B

    .line 41
    .line 42
    if-ge v3, v4, :cond_94

    .line 43
    .line 44
    const/4 v11, 0x1

    .line 45
    invoke-virtual {v0, v5, p2, v11}, Lf/j23;->G9(Lf/u28;BI)Lf/u28;

    .line 46
    .line 47
    .line 48
    move-result-object v6

    .line 49
    if-eqz v6, :cond_94

    .line 50
    .line 51
    iget-byte v7, v1, Lf/jd4;->KC1:B

    .line 52
    .line 53
    const/4 v9, 0x0

    .line 54
    const/4 v10, 0x0

    .line 55
    iget-object v4, p0, Lf/ni6;->BU:Lf/mx3;

    .line 56
    .line 57
    const/4 v8, 0x0

    .line 58
    invoke-virtual/range {v4 .. v10}, Lf/mx3;->nW(Lf/u28;Lf/u28;BZZZ)Z

    .line 59
    .line 60
    .line 61
    move-result v4

    .line 62
    if-nez v4, :cond_40

    .line 63
    .line 64
    goto :goto_94

    .line 65
    :cond_40
    invoke-virtual {v5}, Lf/u28;->tz()B

    .line 66
    .line 67
    .line 68
    move-result v4

    .line 69
    invoke-virtual {v6}, Lf/u28;->tz()B

    .line 70
    .line 71
    .line 72
    move-result v7

    .line 73
    if-eq v4, v7, :cond_58

    .line 74
    .line 75
    invoke-virtual {v5}, Lf/u28;->tz()B

    .line 76
    .line 77
    .line 78
    move-result v4

    .line 79
    const/4 v5, -0x1

    .line 80
    if-eq v4, v5, :cond_58

    .line 81
    .line 82
    invoke-virtual {v6}, Lf/u28;->tz()B

    .line 83
    .line 84
    .line 85
    move-result v4

    .line 86
    if-eq v4, v5, :cond_58

    .line 87
    .line 88
    goto :goto_94

    .line 89
    :cond_58
    invoke-virtual {v6}, Lf/u28;->vD()Z

    .line 90
    .line 91
    .line 92
    move-result v4

    .line 93
    invoke-virtual {p1}, Lf/u28;->vD()Z

    .line 94
    .line 95
    .line 96
    move-result v5

    .line 97
    if-ne v4, v5, :cond_87

    .line 98
    .line 99
    invoke-virtual {v6}, Lf/u28;->mu0()S

    .line 100
    .line 101
    .line 102
    move-result v4

    .line 103
    invoke-virtual {p1}, Lf/u28;->mu0()S

    .line 104
    .line 105
    .line 106
    move-result v5

    .line 107
    if-ne v4, v5, :cond_87

    .line 108
    .line 109
    invoke-virtual {v6}, Lf/u28;->Aj()S

    .line 110
    .line 111
    .line 112
    move-result v4

    .line 113
    invoke-virtual {p1}, Lf/u28;->Aj()S

    .line 114
    .line 115
    .line 116
    move-result v5

    .line 117
    if-ne v4, v5, :cond_87

    .line 118
    .line 119
    invoke-virtual {v6}, Lf/u28;->tz()B

    .line 120
    .line 121
    .line 122
    move-result v4

    .line 123
    invoke-virtual {p1}, Lf/u28;->tz()B

    .line 124
    .line 125
    .line 126
    move-result v5

    .line 127
    if-ne v4, v5, :cond_87

    .line 128
    .line 129
    iget-byte p1, v1, Lf/jd4;->KC1:B

    .line 130
    .line 131
    if-eq p2, p1, :cond_86

    .line 132
    .line 133
    iput-byte p2, v1, Lf/jd4;->KC1:B

    .line 134
    .line 135
    :cond_86
    return v11

    .line 136
    :cond_87
    if-eqz p3, :cond_90

    .line 137
    .line 138
    sget-object v4, Lf/dw2;->qW1:[Lf/c67;

    .line 139
    .line 140
    aget-object v4, v4, p2

    .line 141
    .line 142
    invoke-virtual {p3, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 143
    .line 144
    .line 145
    :cond_90
    add-int/lit8 v3, v3, 0x1

    .line 146
    .line 147
    move-object v5, v6

    .line 148
    goto :goto_27

    .line 149
    :cond_94
    :goto_94
    return v2
.end method

.method public final d11()B
    .registers 2

    .line 1
    iget-byte v0, p0, Lf/dw2;->x71:B

    .line 2
    .line 3
    return v0
.end method

.method public final dF1(BSZ)V
    .registers 5

    .line 1
    iget-object v0, p0, Lf/ni6;->oI:Lf/yq3;

    .line 2
    .line 3
    if-nez v0, :cond_e

    .line 4
    .line 5
    new-instance v0, Lf/yq3;

    .line 6
    .line 7
    invoke-direct {v0, p0, p2, p1}, Lf/yq3;-><init>(Lf/ni6;SB)V

    .line 8
    .line 9
    .line 10
    iput-object v0, p0, Lf/ni6;->oI:Lf/yq3;

    .line 11
    .line 12
    iput-boolean p3, v0, Lf/yq3;->r60:Z

    .line 13
    .line 14
    return-void

    .line 15
    :cond_e
    invoke-super {p0, p1, p2, p3}, Lf/ni6;->dF1(BSZ)V

    .line 16
    .line 17
    .line 18
    return-void
.end method

.method public final i70()S
    .registers 2

    .line 1
    iget-short v0, p0, Lf/dw2;->cp:S

    .line 2
    .line 3
    return v0
.end method

.method public final jN1()S
    .registers 2

    .line 1
    iget-short v0, p0, Lf/dw2;->y9:S

    .line 2
    .line 3
    return v0
.end method

.method public final mD()B
    .registers 2

    .line 1
    iget-byte v0, p0, Lf/dw2;->Fs0:B

    .line 2
    .line 3
    return v0
.end method

.method public final o01(Z)V
    .registers 15

    .line 1
    invoke-super {p0, p1}, Lf/ni6;->o01(Z)V

    .line 2
    .line 3
    .line 4
    if-eqz p1, :cond_f

    .line 5
    .line 6
    iget-object p1, p0, Lf/dw2;->iw:Lf/kn5;

    .line 7
    .line 8
    invoke-virtual {p1}, Lf/kn5;->Hs0()Z

    .line 9
    .line 10
    .line 11
    move-result p1

    .line 12
    if-nez p1, :cond_f

    .line 13
    .line 14
    goto/16 :goto_b5

    .line 15
    .line 16
    :cond_f
    iget-object p1, p0, Lf/ni6;->pQ1:Lf/rp3;

    .line 17
    .line 18
    iget-boolean p1, p1, Lf/rp3;->MA0:Z

    .line 19
    .line 20
    if-nez p1, :cond_17

    .line 21
    .line 22
    goto/16 :goto_b5

    .line 23
    .line 24
    :cond_17
    iget-object p1, p0, Lf/ni6;->BU:Lf/mx3;

    .line 25
    .line 26
    invoke-virtual {p1}, Lf/mx3;->tc0()Z

    .line 27
    .line 28
    .line 29
    move-result v0

    .line 30
    if-eqz v0, :cond_b5

    .line 31
    .line 32
    iget-wide v0, p0, Lf/dw2;->h91:J

    .line 33
    .line 34
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    .line 35
    .line 36
    .line 37
    move-result-wide v2

    .line 38
    cmp-long v4, v0, v2

    .line 39
    .line 40
    if-lez v4, :cond_2b

    .line 41
    .line 42
    goto/16 :goto_b5

    .line 43
    .line 44
    :cond_2b
    iget-object v0, p0, Lf/ni6;->YO:Lf/jd4;

    .line 45
    .line 46
    iget-byte v1, v0, Lf/jd4;->oD0:B

    .line 47
    .line 48
    invoke-static {v1}, Lf/o80;->DU(B)Z

    .line 49
    .line 50
    .line 51
    move-result v1

    .line 52
    if-eqz v1, :cond_41

    .line 53
    .line 54
    iget-short v1, p0, Lf/dw2;->y9:S

    .line 55
    .line 56
    const/16 v2, 0x5c

    .line 57
    .line 58
    if-lt v1, v2, :cond_41

    .line 59
    .line 60
    const/16 v2, 0x6c

    .line 61
    .line 62
    if-gt v1, v2, :cond_41

    .line 63
    .line 64
    goto/16 :goto_b5

    .line 65
    .line 66
    :cond_41
    iget-object v1, p0, Lf/dw2;->iw:Lf/kn5;

    .line 67
    .line 68
    iget v2, p0, Lf/dw2;->jk:I

    .line 69
    .line 70
    iget-byte v3, p0, Lf/dw2;->JK:B

    .line 71
    .line 72
    iget-byte v4, p0, Lf/dw2;->Sq:B

    .line 73
    .line 74
    invoke-virtual {v1, v2, v3, v4}, Lf/kn5;->Dw0(III)Lf/c67;

    .line 75
    .line 76
    .line 77
    move-result-object v1

    .line 78
    const/4 v2, 0x1

    .line 79
    if-eqz v1, :cond_a6

    .line 80
    .line 81
    iget-byte v3, v1, Lf/c67;->Mo:B

    .line 82
    .line 83
    if-lez v3, :cond_8b

    .line 84
    .line 85
    iget-short v4, v0, Lf/jd4;->mI:S

    .line 86
    .line 87
    iget-short v0, v0, Lf/jd4;->Jn1:S

    .line 88
    .line 89
    iget-byte v5, v1, Lf/c67;->kK:B

    .line 90
    .line 91
    if-eq v5, v2, :cond_61

    .line 92
    .line 93
    if-nez v5, :cond_5f

    .line 94
    .line 95
    goto :goto_61

    .line 96
    :cond_5f
    :goto_5f
    move v8, v0

    .line 97
    goto :goto_68

    .line 98
    :cond_61
    :goto_61
    if-ne v5, v2, :cond_66

    .line 99
    .line 100
    sub-int/2addr v0, v3

    .line 101
    :goto_64
    int-to-short v0, v0

    .line 102
    goto :goto_5f

    .line 103
    :cond_66
    add-int/2addr v0, v3

    .line 104
    goto :goto_64

    .line 105
    :goto_68
    const/4 v0, 0x2

    .line 106
    if-eq v5, v0, :cond_71

    .line 107
    .line 108
    const/4 v6, 0x3

    .line 109
    if-ne v5, v6, :cond_6f

    .line 110
    .line 111
    goto :goto_71

    .line 112
    :cond_6f
    :goto_6f
    move v7, v4

    .line 113
    goto :goto_78

    .line 114
    :cond_71
    :goto_71
    if-ne v5, v0, :cond_76

    .line 115
    .line 116
    sub-int/2addr v4, v3

    .line 117
    :goto_74
    int-to-short v4, v4

    .line 118
    goto :goto_6f

    .line 119
    :cond_76
    add-int/2addr v4, v3

    .line 120
    goto :goto_74

    .line 121
    :goto_78
    iget-object v6, p0, Lf/dw2;->iw:Lf/kn5;

    .line 122
    .line 123
    iget-object v0, p0, Lf/dw2;->ra:Lf/jd4;

    .line 124
    .line 125
    iget-short v9, v0, Lf/jd4;->mI:S

    .line 126
    .line 127
    iget-short v10, v0, Lf/jd4;->Jn1:S

    .line 128
    .line 129
    iget-byte v11, p0, Lf/dw2;->JK:B

    .line 130
    .line 131
    iget-byte v12, p0, Lf/dw2;->Sq:B

    .line 132
    .line 133
    invoke-virtual/range {v6 .. v12}, Lf/kn5;->b02(SSSSII)Z

    .line 134
    .line 135
    .line 136
    move-result v0

    .line 137
    if-nez v0, :cond_8b

    .line 138
    .line 139
    goto :goto_b5

    .line 140
    :cond_8b
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    .line 141
    .line 142
    .line 143
    move-result-wide v3

    .line 144
    iget-object v0, p0, Lf/dw2;->iw:Lf/kn5;

    .line 145
    .line 146
    invoke-virtual {v0}, Lf/kn5;->gT0()I

    .line 147
    .line 148
    .line 149
    move-result v0

    .line 150
    int-to-long v5, v0

    .line 151
    add-long/2addr v3, v5

    .line 152
    iput-wide v3, p0, Lf/dw2;->h91:J

    .line 153
    .line 154
    const/4 v0, 0x0

    .line 155
    invoke-virtual {p1, v1, v0}, Lf/mx3;->Kq1(Lf/c67;Z)Z

    .line 156
    .line 157
    .line 158
    move-result p1

    .line 159
    if-eqz p1, :cond_b5

    .line 160
    .line 161
    :goto_a0
    iget p1, p0, Lf/dw2;->jk:I

    .line 162
    .line 163
    add-int/2addr p1, v2

    .line 164
    iput p1, p0, Lf/dw2;->jk:I

    .line 165
    .line 166
    return-void

    .line 167
    :cond_a6
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    .line 168
    .line 169
    .line 170
    move-result-wide v0

    .line 171
    iget-object p1, p0, Lf/dw2;->iw:Lf/kn5;

    .line 172
    .line 173
    invoke-virtual {p1}, Lf/kn5;->gT0()I

    .line 174
    .line 175
    .line 176
    move-result p1

    .line 177
    int-to-long v3, p1

    .line 178
    add-long/2addr v0, v3

    .line 179
    iput-wide v0, p0, Lf/dw2;->h91:J

    .line 180
    .line 181
    goto :goto_a0

    .line 182
    :cond_b5
    :goto_b5
    return-void
.end method

.method public final os0()Z
    .registers 2

    .line 1
    iget-boolean v0, p0, Lf/dw2;->Bu:Z

    .line 2
    .line 3
    return v0
.end method

.method public oy()B
    .registers 3

    .line 1
    iget-boolean v0, p0, Lf/dw2;->kP0:Z

    .line 2
    .line 3
    if-eqz v0, :cond_c

    .line 4
    .line 5
    iget-object v0, p0, Lf/dw2;->UR0:Lf/cc6;

    .line 6
    .line 7
    iget-byte v0, v0, Lf/cc6;->AA:B

    .line 8
    .line 9
    if-eqz v0, :cond_c

    .line 10
    .line 11
    const/4 v0, 0x0

    .line 12
    return v0

    .line 13
    :cond_c
    iget-byte v0, p0, Lf/dw2;->Br0:B

    .line 14
    .line 15
    if-ltz v0, :cond_11

    .line 16
    .line 17
    return v0

    .line 18
    :cond_11
    iget-byte v0, p0, Lf/dw2;->Fs0:B

    .line 19
    .line 20
    const/4 v1, 0x2

    .line 21
    if-ne v0, v1, :cond_23

    .line 22
    .line 23
    sget-object v0, Lf/p37;->T10:Lf/zw0;

    .line 24
    .line 25
    iget-object v0, v0, Lf/zw0;->pH1:Lf/v67;

    .line 26
    .line 27
    iget-short v1, p0, Lf/dw2;->y9:S

    .line 28
    .line 29
    invoke-virtual {v0, v1}, Lf/v67;->Ws1(S)Lf/q06;

    .line 30
    .line 31
    .line 32
    move-result-object v0

    .line 33
    iget-byte v0, v0, Lf/q06;->eg:B

    .line 34
    .line 35
    return v0

    .line 36
    :cond_23
    iget-object v0, p0, Lf/dw2;->UR0:Lf/cc6;

    .line 37
    .line 38
    iget-byte v0, v0, Lf/cc6;->IM0:B

    .line 39
    .line 40
    return v0
.end method

.method public final wy0()F
    .registers 3

    .line 1
    iget-boolean v0, p0, Lf/dw2;->kP0:Z

    .line 2
    .line 3
    iget-object v1, p0, Lf/ni6;->YO:Lf/jd4;

    .line 4
    .line 5
    if-eqz v0, :cond_e

    .line 6
    .line 7
    invoke-virtual {v1}, Lf/jd4;->Kl0()F

    .line 8
    .line 9
    .line 10
    move-result v0

    .line 11
    const/high16 v1, 0x40000000    # 2.0f

    .line 12
    .line 13
    sub-float/2addr v0, v1

    .line 14
    return v0

    .line 15
    :cond_e
    invoke-virtual {v1}, Lf/jd4;->Kl0()F

    .line 16
    .line 17
    .line 18
    move-result v0

    .line 19
    return v0
.end method
