package com.almasb.fxglgames.geowars;

import com.almasb.fxgl.core.math.FXGLMath;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.dsl.components.*;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.particle.ParticleEmitters;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxglgames.geowars.component.*;
import com.almasb.fxglgames.geowars.component.enemy.BouncerComponent;
import com.almasb.fxglgames.geowars.component.enemy.NewRunnerComponent;
import com.almasb.fxglgames.geowars.component.enemy.SeekerComponent;
import com.almasb.fxglgames.geowars.component.enemy.WandererComponent;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.effect.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;

import java.util.Arrays;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.almasb.fxglgames.geowars.GeoWarsType.*;

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
public class GeoWarsFactory implements EntityFactory {

    private final GeoWarsConfig config;

    public GeoWarsFactory() {
        config = new GeoWarsConfig();
    }

    private static final int SPAWN_DISTANCE = 50;

    /**
     * These correspond to top-left, top-right, bottom-right, bottom-left.
     */
    private Point2D[] spawnPoints = new Point2D[] {
            new Point2D(SPAWN_DISTANCE, SPAWN_DISTANCE),
            new Point2D(getAppWidth() - SPAWN_DISTANCE, SPAWN_DISTANCE),
            new Point2D(getAppWidth() - SPAWN_DISTANCE, getAppHeight() - SPAWN_DISTANCE),
            new Point2D(SPAWN_DISTANCE, getAppHeight() - SPAWN_DISTANCE)
    };

    private Point2D getRandomSpawnPoint() {
        return spawnPoints[FXGLMath.random(0, 3)];
    }

    @Spawns("Background")
    public Entity spawnBackground(SpawnData data) {
        Canvas canvas = new Canvas(getAppWidth(), getAppHeight());

        return entityBuilder(data)
                .type(GRID)
                .view(canvas)
                .with(new GraphicsUpdateComponent(canvas.getGraphicsContext2D()))
                .with(new GridComponent(canvas.getGraphicsContext2D()))
                .build();
    }

    @Spawns("BackgroundCircle")
    public Entity spawnBackgroundCircle(SpawnData data) {
        var radius = random(60.0, 100.0);
        Circle circle = new Circle(radius, radius, radius, Color.color(0.2, 0.6, 0.7, 0.5));

        circle.setStrokeType(StrokeType.OUTSIDE);
        circle.setStroke(Color.web("white", 0.3f));
        circle.setStrokeWidth(3);
        circle.setEffect(new BoxBlur(5, 5, 3));

        return entityBuilder(data)
                .view(circle)
                .rotationOrigin(radius, radius)
                .with(new RandomMoveComponent(new Rectangle2D(-200, -200, 200, getAppHeight() + 400), random(1, 15)))
                .build();
    }

    @Spawns("Player")
    public Entity spawnPlayer(SpawnData data) {
        DropShadow ds = new DropShadow();
        ds.setOffsetY(5.0);
        ds.setOffsetX(5.0);
        ds.setColor(Color.GRAY);

        var texture = texture("Player.png");
        texture.setEffect(new Bloom(0.7));

        var e = entityBuilder()
                .type(PLAYER)
                .at(getAppWidth() / 2, getAppHeight() / 2)
                .viewWithBBox(texture)
                .collidable()
                //.with(new KeepOnScreenComponent().bothAxes())
                .with(new PlayerComponent(config.getPlayerSpeed()))
                .build();

        if (!getSettings().isExperimentalNative()) {
            e.addComponent(new ExhaustParticleComponent(ParticleEmitters.newExplosionEmitter(1)));
        }

        return e;
    }

    @Spawns("Bullet")
    public Entity spawnBullet(SpawnData data) {
        if (!getSettings().isExperimentalNative()) {
            play("shoot" + (int) (Math.random() * 8 + 1) + ".wav");
        }

        var name = FXGLMath.random(Arrays.asList("muzzle_01.png", "muzzle_02.png", "muzzle_03.png")).get();

        var w = 96;
        var h = 96;

        var t2 = texture("particles/" + name, w, h).multiplyColor(Color.BLUE.brighter());
        t2.setBlendMode(BlendMode.ADD);
        t2.setTranslateX(-(w / 2.0 - 49 / 2.0));
        t2.setTranslateY(-(h / 2.0 - 13 / 2.0));
        //t2.setEffect(new BoxBlur(15, 15, 3));


        return entityBuilder(data)
                .type(BULLET)
                .viewWithBBox("Bullet.png")
                .view(t2)
                .with(new CollidableComponent(true))
                .with(new ProjectileComponent(data.get("direction"), 1200))
                .with(new BulletComponent())
                .with(new OffscreenCleanComponent())
                .build();
    }

    @Spawns("Wanderer")
    public Entity spawnWanderer(SpawnData data) {
        boolean red = FXGLMath.randomBoolean((float)config.getRedEnemyChance());

        int moveSpeed = red ? config.getRedEnemyMoveSpeed()
                : FXGLMath.random(100, config.getWandererMaxMoveSpeed());

        var t = texture(red ? "RedWanderer.png" : "Wanderer.png", 80, 80).brighter();

        var name = "spark_04.png";

        var w = 128;
        var h = 128;

        var t2 = texture("particles/" + name, w, h).multiplyColor(Color.BLUE.brighter());
        t2.setBlendMode(BlendMode.ADD);
        t2.setTranslateX(-(w / 2.0 - 80 / 2.0));
        t2.setTranslateY(-(h / 2.0 - 80 / 2.0));
        //t2.setEffect(new BoxBlur(15, 15, 3));

        return entityBuilder()
                .type(WANDERER)
                .at(getRandomSpawnPoint())
                .bbox(new HitBox(new Point2D(20, 20), BoundingShape.box(40, 40)))
                //.view(t2)
                .view(t)
                .with(new HealthIntComponent(red ? config.getRedEnemyHealth() : config.getEnemyHealth()))
                .with(new CollidableComponent(true))
                .with(new WandererComponent(moveSpeed, t, texture("wanderer_overlay.png", 80, 80)))
                .build();
    }

    @Spawns("Seeker")
    public Entity spawnSeeker(SpawnData data) {
        boolean red = FXGLMath.randomBoolean((float)config.getRedEnemyChance());

        int moveSpeed = red ? config.getRedEnemyMoveSpeed()
                : FXGLMath.random(150, config.getSeekerMaxMoveSpeed());

        // TODO: red ? "RedSeeker.png" : "Seeker.png"

        return entityBuilder()
                .type(SEEKER)
                .at(getRandomSpawnPoint())
                .viewWithBBox(texture("Seeker.png", 60, 60).brighter())
                .with(new HealthIntComponent(red ? config.getRedEnemyHealth() : config.getEnemyHealth()))
                .with(new CollidableComponent(true))
                .with(new SeekerComponent(FXGL.<GeoWarsApp>getAppCast().getPlayer(), moveSpeed))
                .build();
    }

    @Spawns("Runner")
    public Entity spawnRunner(SpawnData data) {
        return entityBuilder()
                .type(RUNNER)
                .at(getRandomSpawnPoint())
                .viewWithBBox(texture("Runner.png", 258 * 0.25, 220 * 0.25))
                .with(new HealthIntComponent(config.getEnemyHealth()))
                .with(new CollidableComponent(true))
                .with(new NewRunnerComponent(config.getRunnerMoveSpeed()))
                .with(new AutoRotationComponent().withSmoothing())
                //.with(new RunnerComponent(config.getRunnerMoveSpeed()))
                //.with(new RandomMoveComponent(new Rectangle2D(0, 0, getAppWidth(), getAppHeight()), config.getRunnerMoveSpeed(), FXGLMath.random(250, 500)))
                .build();
    }

    @Spawns("Bouncer")
    public Entity spawnBouncer(SpawnData data) {
        double y = FXGLMath.random(0, getAppHeight() - 40);

        return entityBuilder()
                .type(BOUNCER)
                .at(0, y)
                .viewWithBBox(texture("Bouncer.png", 254 * 0.25, 304 * 0.25))
                .with(new HealthIntComponent(config.getEnemyHealth()))
                .with(new CollidableComponent(true))
                .with(new BouncerComponent(config.getBouncerMoveSpeed()))
                .build();
    }

    @Spawns("Explosion")
    public Entity spawnExplosion(SpawnData data) {
        var e = entityBuilder()
                .at(data.getX() - 40, data.getY() - 40)
                .view(texture("explosion.png", 80 * 48, 80).toAnimatedTexture(48, Duration.seconds(0.75)).play())
                .with(new ExpireCleanComponent(Duration.seconds(1.6)))
                .build();

        if (!getSettings().isExperimentalNative()) {
            e.addComponent(new ExplosionParticleComponent());

            play("explosion-0" + (int) (Math.random() * 8 + 1) + ".wav");
        }

        return e;
    }

    @Spawns("Portal")
    public Entity spawnPortal(SpawnData data) {
        return entityBuilder(data)
                .type(PORTAL)
                .viewWithBBox("Portal.png")
                .with(new CollidableComponent(true))
                .with(new ExpireCleanComponent(Duration.seconds(10)))
                .build();
    }

    @Spawns("Crystal")
    public Entity spawnCrystal(SpawnData data) {
        var name = "light_02.png";

        var w = 64;
        var h = 64;

        var t = texture("particles/" + name, w, h).multiplyColor(Color.YELLOW.brighter());
        t.setBlendMode(BlendMode.ADD);
        t.setTranslateX(-(w / 2.0 - 32 / 2.0));
        t.setTranslateY(-(h / 2.0 - 32 / 2.0));
        t.setEffect(new BoxBlur(15, 15, 3));

        return entityBuilder(data)
                .type(CRYSTAL)
                .scale(0.65, 0.65)
                .view(t)
                .viewWithBBox(texture("YellowCrystal.png").toAnimatedTexture(8, Duration.seconds(1)))
                .with(new CollidableComponent(true))
                .with(new CrystalComponent(), new ExpireCleanComponent(Duration.seconds(10)))
                .build();
    }
}
