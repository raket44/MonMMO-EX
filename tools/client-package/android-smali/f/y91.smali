.class public final Lf/y91;
.super Ljava/lang/Object;


# static fields
.field public static Gl1:Lf/y91;


# instance fields
.field public final Uv:Ljava/util/HashMap;

.field public final XU0:Ljava/util/HashMap;


# direct methods
.method static constructor <clinit>()V
    .registers 1

    .line 1
    new-instance v0, Lf/y91;

    .line 2
    .line 3
    invoke-direct {v0}, Lf/y91;-><init>()V

    .line 4
    .line 5
    .line 6
    sput-object v0, Lf/y91;->Gl1:Lf/y91;

    .line 7
    .line 8
    return-void
.end method

.method public constructor <init>()V
    .registers 2

    .line 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 2
    .line 3
    .line 4
    new-instance v0, Ljava/util/HashMap;

    .line 5
    .line 6
    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    .line 7
    .line 8
    .line 9
    iput-object v0, p0, Lf/y91;->Uv:Ljava/util/HashMap;

    .line 10
    .line 11
    new-instance v0, Ljava/util/HashMap;

    .line 12
    .line 13
    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    .line 14
    .line 15
    .line 16
    iput-object v0, p0, Lf/y91;->XU0:Ljava/util/HashMap;

    .line 17
    .line 18
    return-void
.end method

.method public static xk0()Lf/y91;
    .registers 1

    .line 1
    sget-object v0, Lf/y91;->Gl1:Lf/y91;

    .line 2
    .line 3
    if-nez v0, :cond_b

    .line 4
    .line 5
    new-instance v0, Lf/y91;

    .line 6
    .line 7
    invoke-direct {v0}, Lf/y91;-><init>()V

    .line 8
    .line 9
    .line 10
    sput-object v0, Lf/y91;->Gl1:Lf/y91;

    .line 11
    .line 12
    :cond_b
    sget-object v0, Lf/y91;->Gl1:Lf/y91;

    .line 13
    .line 14
    return-object v0
.end method


# virtual methods
.method public final aE([S)Ljava/util/ArrayList;
    .registers 6

    .line 1
    new-instance v0, Ljava/util/ArrayList;

    .line 2
    .line 3
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 4
    .line 5
    .line 6
    iget-object v1, p0, Lf/y91;->Uv:Ljava/util/HashMap;

    .line 7
    .line 8
    invoke-virtual {v1}, Ljava/util/HashMap;->values()Ljava/util/Collection;

    .line 9
    .line 10
    .line 11
    move-result-object v1

    .line 12
    invoke-interface {v1}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    .line 13
    .line 14
    .line 15
    move-result-object v1

    .line 16
    :cond_f
    :goto_f
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    .line 17
    .line 18
    .line 19
    move-result v2

    .line 20
    if-eqz v2, :cond_2c

    .line 21
    .line 22
    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 23
    .line 24
    .line 25
    move-result-object v2

    .line 26
    check-cast v2, Lf/zp3;

    .line 27
    .line 28
    iget-object v3, v2, Lf/zp3;->pRn:Lf/zp3;

    .line 29
    .line 30
    if-nez v3, :cond_20

    .line 31
    .line 32
    move-object v3, v2

    .line 33
    :cond_20
    iget-short v3, v3, Lf/zp3;->Kj1:S

    .line 34
    .line 35
    invoke-static {v3, p1}, Lf/qy4;->JH0(S[S)Z

    .line 36
    .line 37
    .line 38
    move-result v3

    .line 39
    if-eqz v3, :cond_f

    .line 40
    .line 41
    invoke-virtual {v0, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 42
    .line 43
    .line 44
    goto :goto_f

    .line 45
    :cond_2c
    return-object v0
.end method

.method public final fp(B)Ljava/util/ArrayList;
    .registers 7

    .line 1
    new-instance v0, Ljava/util/ArrayList;

    .line 2
    .line 3
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 4
    .line 5
    .line 6
    iget-object v1, p0, Lf/y91;->XU0:Ljava/util/HashMap;

    # MonMMO-EX: the Pokedex map is filled once with ids 1-649 up to the first gap, so Expansion
    # species never reach it. Re-copy the full registry each time a list is built.
    iget-object v2, p0, Lf/y91;->Uv:Ljava/util/HashMap;
    invoke-virtual {v1}, Ljava/util/HashMap;->clear()V
    invoke-virtual {v1, v2}, Ljava/util/HashMap;->putAll(Ljava/util/Map;)V

    .line 7
    .line 8
    invoke-virtual {v1}, Ljava/util/HashMap;->values()Ljava/util/Collection;

    .line 9
    .line 10
    .line 11
    move-result-object v1

    .line 12
    invoke-interface {v1}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;

    .line 13
    .line 14
    .line 15
    move-result-object v1

    .line 16
    :cond_f
    :goto_f
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    .line 17
    .line 18
    .line 19
    move-result v2

    .line 20
    if-eqz v2, :cond_2e

    .line 21
    .line 22
    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 23
    .line 24
    .line 25
    move-result-object v2

    .line 26
    check-cast v2, Lf/zp3;

    .line 27
    .line 28
    invoke-virtual {v2, p1}, Lf/zp3;->Wi(B)S

    .line 29
    .line 30
    .line 31
    move-result v3

    .line 32
    if-gtz v3, :cond_2a

    .line 33
    .line 34
    const/4 v3, 0x2

    .line 35
    if-ne p1, v3, :cond_f

    .line 36
    .line 37
    iget-short v3, v2, Lf/zp3;->Kj1:S

    .line 38
    .line 39
    const/16 v4, 0x1ee

    .line 40
    .line 41
    if-ne v3, v4, :cond_f

    .line 42
    .line 43
    :cond_2a
    invoke-virtual {v0, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 44
    .line 45
    .line 46
    goto :goto_f

    .line 47
    :cond_2e
    new-instance v1, Lf/az3;

    .line 48
    .line 49
    invoke-direct {v1, p1}, Lf/az3;-><init>(B)V

    .line 50
    .line 51
    .line 52
    invoke-static {v1}, Lj$/util/Comparator$-CC;->comparingInt(Ljava/util/function/ToIntFunction;)Ljava/util/Comparator;

    .line 53
    .line 54
    .line 55
    move-result-object p1

    .line 56
    invoke-static {v0, p1}, Ljava/util/Collections;->sort(Ljava/util/List;Ljava/util/Comparator;)V

    .line 57
    .line 58
    .line 59
    return-object v0
.end method

.method public final wT0(S)Lf/zp3;
    .registers 3

    .line 1
    iget-object v0, p0, Lf/y91;->Uv:Ljava/util/HashMap;

    .line 2
    .line 3
    invoke-static {p1}, Ljava/lang/Short;->valueOf(S)Ljava/lang/Short;

    .line 4
    .line 5
    .line 6
    move-result-object p1

    .line 7
    invoke-virtual {v0, p1}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    .line 8
    .line 9
    .line 10
    move-result-object p1

    .line 11
    check-cast p1, Lf/zp3;

    .line 12
    .line 13
    return-object p1
.end method
