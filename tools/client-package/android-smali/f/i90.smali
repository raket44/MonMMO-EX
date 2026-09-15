.class public final Lf/i90;
.super Lf/if3;


# instance fields
.field public Qt1:Lf/cj1;

.field public final aM:B

.field public final fT:Lf/iy;

.field public final or0:Lf/r59;

.field public final sY1:Lf/b54;

.field public final uj:Z

.field public y71:Lf/b54;


# direct methods
.method public constructor <init>(Lf/iy;Lf/b54;Lf/b54;Lf/r59;BZ)V
    .registers 7

    .line 1
    invoke-direct {p0}, Lf/if3;-><init>()V

    .line 2
    .line 3
    .line 4
    iput-object p1, p0, Lf/i90;->fT:Lf/iy;

    .line 5
    .line 6
    iput-object p2, p0, Lf/i90;->y71:Lf/b54;

    .line 7
    .line 8
    iput-object p3, p0, Lf/i90;->sY1:Lf/b54;

    .line 9
    .line 10
    iput-object p4, p0, Lf/i90;->or0:Lf/r59;

    .line 11
    .line 12
    iput-byte p5, p0, Lf/i90;->aM:B

    .line 13
    .line 14
    iput-boolean p6, p0, Lf/i90;->uj:Z

    .line 15
    .line 16
    return-void
.end method


# virtual methods
.method public final Rv0()Lf/x98;
    .registers 2

    .line 1
    sget-object v0, Lf/x98;->Ty:Lf/x98;

    .line 2
    .line 3
    return-object v0
.end method

.method public final Vr()Z
    .registers 2

    .line 1
    sget-object v0, Lf/p37;->Y8:Lf/q97;

    .line 2
    .line 3
    iget-object v0, v0, Lf/nq7;->e60:Lf/LpT6;

    .line 4
    .line 5
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 6
    .line 7
    .line 8
    instance-of v0, v0, Lf/ap5;

    .line 9
    .line 10
    if-eqz v0, :cond_c

    .line 11
    .line 12
    goto :goto_1f

    .line 13
    :cond_c
    iget-object v0, p0, Lf/i90;->y71:Lf/b54;

    .line 14
    .line 15
    if-nez v0, :cond_15

    .line 16
    .line 17
    iget-object v0, p0, Lf/i90;->sY1:Lf/b54;

    .line 18
    .line 19
    if-nez v0, :cond_15

    .line 20
    .line 21
    goto :goto_1f

    .line 22
    :cond_15
    iget-object v0, p0, Lf/i90;->Qt1:Lf/cj1;

    .line 23
    .line 24
    if-eqz v0, :cond_21

    .line 25
    .line 26
    invoke-virtual {v0}, Lf/cj1;->EV()Z

    .line 27
    .line 28
    .line 29
    move-result v0

    .line 30
    if-eqz v0, :cond_21

    .line 31
    .line 32
    :goto_1f
    const/4 v0, 0x1

    .line 33
    return v0

    .line 34
    :cond_21
    const/4 v0, 0x0

    .line 35
    return v0
.end method

.method public final tn0()V
    .registers 9

    .line 1
    iget-object v0, p0, Lf/i90;->y71:Lf/b54;

    .line 2
    .line 3
    iget-object v1, p0, Lf/i90;->sY1:Lf/b54;

    .line 4
    .line 5
    if-nez v0, :cond_8

    .line 6
    .line 7
    iput-object v1, p0, Lf/i90;->y71:Lf/b54;

    .line 8
    .line 9
    :cond_8
    sget-object v0, Lf/p37;->Y8:Lf/q97;

    .line 10
    .line 11
    iget-object v0, v0, Lf/nq7;->e60:Lf/LpT6;

    .line 12
    .line 13
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 14
    .line 15
    .line 16
    instance-of v0, v0, Lf/ap5;

    .line 17
    .line 18
    if-eqz v0, :cond_14

    .line 19
    .line 20
    return-void

    .line 21
    :cond_14
    iget-boolean v0, p0, Lf/i90;->uj:Z

    .line 22
    .line 23
    const/high16 v2, 0x3f800000    # 1.0f

    .line 24
    .line 25
    if-eqz v0, :cond_a8

    .line 26
    .line 27
    new-instance v0, Lf/sw2;

    .line 28
    .line 29
    iget-object v3, p0, Lf/i90;->y71:Lf/b54;

    .line 30
    .line 31
    invoke-direct {v0, v3}, Lf/cj1;-><init>(Lf/b54;)V

    .line 32
    .line 33
    .line 34
    iget-object v3, p0, Lf/i90;->fT:Lf/iy;

    .line 35
    .line 36
    iput-object v3, v0, Lf/sw2;->Zw0:Lf/iy;

    .line 37
    .line 38
    invoke-virtual {v0, v1}, Lf/cj1;->Pw1(Lf/b54;)Lf/cj1;

    .line 39
    .line 40
    .line 41
    iget-byte v1, p0, Lf/i90;->aM:B

    .line 42
    .line 43
    if-nez v1, :cond_2e

    .line 44
    .line 45
    goto/16 :goto_a5

    .line 46
    .line 47
    :cond_2e
    if-lez v1, :cond_32

    .line 48
    .line 49
    const/4 v3, 0x1

    .line 50
    goto :goto_33

    .line 51
    :cond_32
    const/4 v3, 0x0

    .line 52
    :goto_33
    if-eqz v3, :cond_38

    .line 53
    .line 54
    const-string v4, "up"

    .line 55
    .line 56
    goto :goto_3a

    .line 57
    :cond_38
    const-string v4, "down"

    .line 58
    .line 59
    :goto_3a
    const-string v5, "status/"

    .line 60
    .line 61
    invoke-virtual {v5, v4}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    .line 62
    .line 63
    .line 64
    move-result-object v4

    .line 65
    invoke-virtual {v0, v4}, Lf/cj1;->bl1(Ljava/lang/String;)Lcom/badlogic/gdx/graphics/g3d/particles/ParticleEffectExt;

    .line 66
    .line 67
    .line 68
    move-result-object v4

    # MonMMO-EX 2026-09-15: anchor the stat particles CASTER -> ENEMY (the target monster, whose
    # position bl1 already set from D50). Retail leaves the controller default CASTER -> ABSOLUTE,
    # and in that mode PolarAccelerationExt applies status/down.vfx's dir (0,-1,0) x strength -5.523
    # literally, so a stat DROP drifts upward. With an ENEMY target afterInit takes the
    # caster->enemy branch, a pure-y dir zeroes out, and update() falls back to the (0,1,0) axis x
    # strength: down falls, up still rises - the motion both files were authored for. v5/v6 are
    # free here (reassigned right below).
    sget-object v5, Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;->CASTER:Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;
    sget-object v6, Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;->ENEMY:Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;
    invoke-virtual {v4, v5, v6}, Lcom/badlogic/gdx/graphics/g3d/particles/ParticleEffectExt;->setAllControllersTargets(Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;)V

    .line 69
    new-instance v5, Ljava/util/HashSet;

    .line 70
    .line 71
    invoke-direct {v5}, Ljava/util/HashSet;-><init>()V

    .line 72
    .line 73
    .line 74
    invoke-static {}, Lf/ra9;->gi()Lf/ra9;

    .line 75
    .line 76
    .line 77
    move-result-object v6

    .line 78
    const/high16 v7, 0x3f400000    # 0.75f

    .line 79
    .line 80
    invoke-virtual {v6, v7}, Lf/ra9;->ZI1(F)V

    .line 81
    .line 82
    .line 83
    invoke-virtual {v6}, Lf/ra9;->Gh0()V

    .line 84
    .line 85
    .line 86
    if-eqz v3, :cond_5a

    .line 87
    .line 88
    const/16 v3, 0x615

    .line 89
    .line 90
    goto :goto_5c

    .line 91
    :cond_5a
    const/16 v3, 0x616

    .line 92
    .line 93
    :goto_5c
    iget-object v7, v0, Lf/cj1;->D51:Lf/b54;

    .line 94
    .line 95
    invoke-virtual {v7}, Lf/b54;->sP0()Z

    .line 96
    .line 97
    .line 98
    move-result v7

    .line 99
    invoke-static {v3, v7}, Lf/cj1;->wu1(SZ)Lf/jp6;

    .line 100
    .line 101
    .line 102
    move-result-object v3

    .line 103
    invoke-virtual {v6, v3}, Lf/ra9;->fg0(Lf/jp6;)V

    .line 104
    .line 105
    .line 106
    new-instance v3, Lf/w99;

    .line 107
    .line 108
    const/16 v7, 0x13

    .line 109
    .line 110
    invoke-direct {v3, v0, v7, v4}, Lf/w99;-><init>(Ljava/lang/Object;ILjava/lang/Object;)V

    .line 111
    .line 112
    .line 113
    invoke-static {v3}, Lf/jp6;->cB1(Lf/oa1;)Lf/jp6;

    .line 114
    .line 115
    .line 116
    move-result-object v3

    .line 117
    invoke-virtual {v6, v3}, Lf/ra9;->fg0(Lf/jp6;)V

    .line 118
    .line 119
    .line 120
    iput-object v6, v0, Lf/cj1;->di0:Lf/ra9;

    .line 121
    .line 122
    iget-object v3, p0, Lf/i90;->or0:Lf/r59;

    .line 123
    .line 124
    if-eqz v3, :cond_92

    .line 125
    .line 126
    invoke-virtual {v6}, Lf/ra9;->K31()V

    .line 127
    .line 128
    .line 129
    invoke-virtual {v6, v2}, Lf/ra9;->ZI1(F)V

    .line 130
    .line 131
    .line 132
    new-instance v2, Lf/q83;

    .line 133
    .line 134
    invoke-direct {v2, v0, v5, v3, v1}, Lf/q83;-><init>(Lf/sw2;Ljava/util/HashSet;Lf/r59;B)V

    .line 135
    .line 136
    .line 137
    invoke-static {v2}, Lf/jp6;->cB1(Lf/oa1;)Lf/jp6;

    .line 138
    .line 139
    .line 140
    move-result-object v1

    .line 141
    invoke-virtual {v6, v1}, Lf/ra9;->fg0(Lf/jp6;)V

    .line 142
    .line 143
    .line 144
    invoke-virtual {v6}, Lf/ra9;->k71()V

    .line 145
    .line 146
    .line 147
    :cond_92
    iget-object v1, v0, Lf/cj1;->di0:Lf/ra9;

    .line 148
    .line 149
    invoke-virtual {v1}, Lf/ra9;->k71()V

    .line 150
    .line 151
    .line 152
    iget-object v1, v0, Lf/cj1;->di0:Lf/ra9;

    .line 153
    .line 154
    iget-object v2, v0, Lf/cj1;->nD0:Lf/x55;

    .line 155
    .line 156
    iget-object v3, v2, Lf/x55;->tE0:Lf/f56;

    .line 157
    .line 158
    invoke-virtual {v1, v3}, Lf/fz6;->iO(Lf/f56;)V

    .line 159
    .line 160
    .line 161
    iget-object v1, v0, Lf/cj1;->di0:Lf/ra9;

    .line 162
    .line 163
    invoke-virtual {v2, v1}, Lf/x55;->wz0(Lf/ra9;)V

    .line 164
    .line 165
    .line 166
    :goto_a5
    iput-object v0, p0, Lf/i90;->Qt1:Lf/cj1;

    .line 167
    .line 168
    goto :goto_b7

    .line 169
    :cond_a8
    new-instance v0, Lf/pz6;

    .line 170
    .line 171
    new-instance v3, Lf/hn8;

    .line 172
    .line 173
    const/4 v4, 0x3

    .line 174
    invoke-direct {v3, v4, p0}, Lf/hn8;-><init>(ILjava/lang/Object;)V

    .line 175
    .line 176
    .line 177
    invoke-direct {v0, v1, v2, v3}, Lf/pz6;-><init>(Lf/b54;FLjava/lang/Runnable;)V

    .line 178
    .line 179
    .line 180
    invoke-virtual {v0}, Lf/pz6;->zl0()Lf/cj1;

    .line 181
    .line 182
    .line 183
    goto :goto_a5

    .line 184
    :goto_b7
    sget-object v0, Lf/p37;->Y8:Lf/q97;

    .line 185
    .line 186
    iget-object v0, v0, Lf/nq7;->e60:Lf/LpT6;

    .line 187
    .line 188
    iget-object v1, p0, Lf/i90;->Qt1:Lf/cj1;

    .line 189
    .line 190
    iput-object v1, v0, Lf/LpT6;->MG:Lf/cj1;

    .line 191
    .line 192
    return-void
.end method
