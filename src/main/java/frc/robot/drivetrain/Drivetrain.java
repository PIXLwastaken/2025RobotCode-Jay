package frc.robot.drivetrain;

import java.util.Optional;

import com.ctre.phoenix6.configs.Pigeon2Configuration;
import com.ctre.phoenix6.hardware.Pigeon2;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.RobotContainer;
import frc.robot.drivetrain.DrivetrainConfig.DriveConstants;
import frc.robot.drivetrain.swerve.SwerveModule;

public class Drivetrain extends SubsystemBase {
    private static Drivetrain mInstance;
    public SwerveDriveOdometry swerveOdometry;
    public SwerveModule[] mSwerveMods;
    public Pigeon2 gyro;

    private SwerveModuleState[] lastTargetStates;
    private Translation2d lastTranslation;
    private double lastRotation;

    public static boolean ampMode, speakerMode;
    private double noteSpeed, avgVelocity;

    private Drivetrain() {
        gyro = new Pigeon2(DriveConstants.pigeonID, "Canivore");
        gyro.getConfigurator().apply(new Pigeon2Configuration());
        gyro.setYaw(0);

        mSwerveMods = new SwerveModule[]{
            new SwerveModule(0, DriveConstants.Mod0.config),
            new SwerveModule(1, DriveConstants.Mod1.config),
            new SwerveModule(2, DriveConstants.Mod2.config),
            new SwerveModule(3, DriveConstants.Mod3.config)
        };

        swerveOdometry = new SwerveDriveOdometry(DriveConstants.swerveKinematics, getGyroYaw(), getModulePositions());
    }

    public static Drivetrain getInstance() {
        if (mInstance == null) {
            mInstance = new Drivetrain();
        }
        return mInstance;
    }

    //MASTER DRIVE METHOD
    public void drive(Translation2d translation, double rotation, boolean fieldRelative, boolean isOpenLoop, boolean modeS, boolean modeA) {
        avgVelocity = 0;
        ampMode = modeA;
        speakerMode = modeS;

        // Note Speed is in m/s
        noteSpeed = 16.0;

        // Used to find avg Velocity of all 4 Mods
        for (SwerveModule mod : mSwerveMods) {
            avgVelocity += Math.abs(mod.getState().speedMetersPerSecond);
        }
        avgVelocity /= 4;

        lastRotation = rotation;
        lastTranslation = translation;

        SwerveModuleState[] swerveModuleStates =
            DriveConstants.swerveKinematics.toSwerveModuleStates(
                fieldRelative ? ChassisSpeeds.fromFieldRelativeSpeeds(
                    translation.getX(),
                    translation.getY(),
                    lastRotation,
                    getHeading()
                )
                    : new ChassisSpeeds(
                    translation.getX(),
                    translation.getY(),
                    rotation)
            );
        SwerveDriveKinematics.desaturateWheelSpeeds(swerveModuleStates, DriveConstants.maxSpeed);

        for (SwerveModule mod : mSwerveMods) {
            mod.setDesiredState(swerveModuleStates[mod.moduleNumber], isOpenLoop);
        }
    }

    public void driveRobotRelative(ChassisSpeeds speeds) {
        SwerveModuleState[] states = DriveConstants.swerveKinematics.toSwerveModuleStates(speeds);
        SwerveDriveKinematics.desaturateWheelSpeeds(states, DriveConstants.maxSpeed);
        setModuleStates(states);
    }

    public Rotation2d getAngularVelocity() {
        return  Rotation2d.fromRotations(gyro.getAngularVelocityZWorld().getValueAsDouble());
    }


    public ChassisSpeeds getRobotRelativeSpeeds() {
        return DriveConstants.swerveKinematics.toChassisSpeeds(getModuleStates());
    }

    public void stopSwerve() {
        Translation2d stop = new Translation2d(0, 0);
        drive(stop, 0, true, true, false, false);
    }

    //USE FOR AUTONOMOUS COMMANDS
    public void setModuleStates(SwerveModuleState[] desiredStates) {
        SwerveDriveKinematics.desaturateWheelSpeeds(desiredStates, DriveConstants.maxSpeed);

        for (SwerveModule mod : mSwerveMods) {
            mod.setDesiredState(desiredStates[mod.moduleNumber], false);
        }
    }

    public SwerveModuleState[] getModuleStates() {
        SwerveModuleState[] states = new SwerveModuleState[4];
        for (SwerveModule mod : mSwerveMods) {
            states[mod.moduleNumber] = mod.getState();
        }
        return states;
    }

    public SwerveModulePosition[] getModulePositions() {
        SwerveModulePosition[] positions = new SwerveModulePosition[4];
        for (SwerveModule mod : mSwerveMods) {
            positions[mod.moduleNumber] = mod.getPosition();
        }
        return positions;
    }

    public Pose2d getPose() {
        return swerveOdometry.getPoseMeters();
    }

    public void setPose(Pose2d pose) {
        swerveOdometry.resetPosition(getGyroYaw(), getModulePositions(), pose);
    }

    public void resetPose(Pose2d pose) {
        swerveOdometry.resetPosition(
            this.getHeading(),
            new SwerveModulePosition[]{
                mSwerveMods[0].getPosition(),
                mSwerveMods[1].getPosition(),
                mSwerveMods[2].getPosition(),
                mSwerveMods[3].getPosition()
            },
            pose
        );
    }

    public Rotation2d getHeading() {
        return getPose().getRotation();
    }

    public void setHeading(Rotation2d heading) {
        swerveOdometry.resetPosition(getGyroYaw(), getModulePositions(), new Pose2d(getPose().getTranslation(), heading));
    }

    public void zeroHeading() {
        swerveOdometry.resetPosition(getGyroYaw(), getModulePositions(), new Pose2d(getPose().getTranslation(), new Rotation2d()));
        
        // FIX LATER
        // Vision.PoseEstimator.getInstance().setCurrentPose(new Pose2d(getPose().getTranslation(), new Rotation2d()));
    }

    public Rotation2d getGyroYaw() {
        return Rotation2d.fromDegrees(gyro.getYaw().getValueAsDouble());
    }

    public void resetModulesToAbsolute() {
        for (SwerveModule mod : mSwerveMods) {
            mod.resetToAbsolute();
        }
    }

    @Override
    public void periodic() {
        swerveOdometry.update(getGyroYaw(), getModulePositions());
        SmartDashboard.putString("odometry", swerveOdometry.getPoseMeters().toString());
        SmartDashboard.putString("requested translation", (lastTranslation != null) ? lastTranslation.toString() : "null");
        SmartDashboard.putNumber("requested rotation", lastRotation);
        SmartDashboard.putNumber("GyroAngle", gyro.getYaw().getValueAsDouble());
        for (SwerveModule mod : mSwerveMods) {
            SmartDashboard.putNumber("Mod " + mod.moduleNumber + " CANcoder", mod.getCANcoder().getDegrees());
            SmartDashboard.putNumber("Mod " + mod.moduleNumber + " Angle", mod.getPosition().angle.getDegrees());
            SmartDashboard.putNumber("Mod " + mod.moduleNumber + " Velocity", Math.abs(mod.getState().speedMetersPerSecond));
        }
    }
}
