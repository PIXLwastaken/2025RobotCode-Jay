package frc.robot.drivetrain;

import java.util.function.Supplier;


import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import com.pathplanner.lib.config.ModuleConfig;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.util.Units;
import frc.crevolib.util.SDSConstants;
import frc.crevolib.util.WCPConstants;
import frc.robot.drivetrain.swerve.SwerveModuleConfig;

public class DrivetrainConfig {
    public class DriveConstants {
        //TODO: figure out PigeonID and change this
        public static final int pigeonID = 13; 

        //SDS Constants Class Made to Easily Switch Module Gear Ratios
        // public static final SDSConstants chosenModule = SDSConstants.MK4i.Falcon500(SDSConstants.MK4i.driveRatios.L3);
        
        //WCP Constants
        public static final WCPConstants chosenModule = WCPConstants.X2t.Falcon500(WCPConstants.X2t.driveRatios.ratio);
        
        // Robot Specific Constants
        public static double robotKG = 40.0;
        public static double MOI = 20.0; // Units = kg*m^2
        public static double COF = 1.2;
        public static int numOfDriveMotors = 1;

        /* Drivetrain Constants */
        //TODO: Change Robot Frame's Width and Length Depending on Frame Size Design makes
        public static final double trackWidth = Units.inchesToMeters(22.75);
        public static final double wheelBase = Units.inchesToMeters(22.75);
        public static final double wheelCircumference = chosenModule.wheelCircumference;
        public static final double wheelRadius = Units.inchesToMeters(3.0);

        /* Swerve Kinematics 
         * Only works for square or rectangular frame (do not worry about this) */
         public static final SwerveDriveKinematics swerveKinematics = new SwerveDriveKinematics(
            new Translation2d(wheelBase / 2.0, trackWidth / 2.0),
            new Translation2d(wheelBase / 2.0, -trackWidth / 2.0),
            new Translation2d(-wheelBase / 2.0, trackWidth / 2.0),
            new Translation2d(-wheelBase / 2.0, -trackWidth / 2.0)
        );

        /* Module offsets from Center of Robot */
        public static final Supplier<Translation2d[]> modsOffSets = () -> new Translation2d[] {
            new Translation2d(-10.5, 10.5),
            new Translation2d(10.5, 10.5),
            new Translation2d(-10.5, -10.5),
            new Translation2d(10.5, -10.5)
        };
        
        
        /* Module Gear Ratios */
        public static final double driveGearRatio = chosenModule.driveGearRatio;
        public static final double angleGearRatio = chosenModule.angleGearRatio;

        /* Motor Inverts */
        public static final InvertedValue angleMotorInvert = chosenModule.angleMotorInvert;
        public static final InvertedValue driveMotorInvert = chosenModule.driveMotorInvert;

        /*Slow Mode Modifiers */
        public static final double kSlowModeTranslationModifier = 0.25;
        public static final double kSlowModeRotationModifier = 0.5;

        /*Intake Mode Modifiers */
        public static final double kIntakeModeTranslationModifier = 0.75;
        public static final double kIntakeModeRotationModifier = 0.6;

        /* Angle Encoder Invert */
        public static final SensorDirectionValue cancoderInvert = chosenModule.cancoderInvert;

        /* Swerve Current Limiting - Angle/Drive Motors */
        public static final int angleCurrentLimit = 25;
        public static final int angleCurrentThreshold = 40;
        public static final double angleCurrentThresholdTime = 0.1;
        public static final boolean angleEnableCurrentLimit = true;

        public static final int driveCurrentLimit = 35;
        public static final int driveCurrentThreshold = 60;
        public static final double driveCurrentThresholdTime = 0.1;
        public static final boolean driveEnableCurrentLimit = true;

      
        /* A small open loop ramp (0.25) helps with tread wear, tipping, etc */
        public static final double openLoopRamp = 0.25;
        public static final double closedLoopRamp = 0.0;

        /* Angle Motor PID Values */
        public static final double angleKP = chosenModule.angleKP;
        public static final double angleKI = chosenModule.angleKI;
        public static final double angleKD = chosenModule.angleKD;

        /* Drive Motor PID Values */
        //TODO: TUNE THIS BASED ON TESTING
        public static final double driveKP = 0.1;
        public static final double driveKI = 0.0;
        public static final double driveKD = 0.1;

        /* Drive Motor Characterization Values From SYSID */
        //TODO: This must be tuned to specific robot using SYSID, But Knight Vision Constants work good :)

        // public static final double driveKS = 0.48665; //2024
        // public static final double driveKV = 2.2; //2024
        // public static final double driveKA = 0.37; //2024

        public static final double driveKS = 0.48665; //2025 - left the same
        public static final double driveKV = 1.82; //2025 - recalc
        public static final double driveKA = 0.44; //2025 - recalc


        /* Swerve Profiling Values */
        /** Meters per Second */
        public static final double maxSpeed = Units.feetToMeters(22.70); // from recalc
        /** Radians per Second */
        public static final double maxAngularVelocity = Math.PI * 4.12 * 0.5;

        /* Neutral Modes */
        public static final NeutralModeValue angleNeutralMode = NeutralModeValue.Coast;
        public static final NeutralModeValue driveNeutralMode = NeutralModeValue.Brake;

        /*Swerve Profiling Values*/
        public static final double MAX_SPEED = (Units.feetToMeters(18.0)); //Max from SDS Limit Speed
        public static final double MAX_ANGULAR_VELOCITY = Math.PI * 4.12 * 0.5;

        public static final DCMotor driveMotorGearBox = DCMotor.getFalcon500(1);

        public static final ModuleConfig modConfig = 
            new ModuleConfig(
                wheelRadius,
                maxSpeed,
                COF, 
                driveMotorGearBox, 
                SDSConstants.MK4i.driveRatios.L3, 
                driveCurrentLimit, 
                numOfDriveMotors
            );

        //TODO: TUNE THESE TO THE IDs FOR EACH MOTOR AND CANCODER and FIGURE OUT THE OFFSET
        /* Module Specific Constants */

        // Front Left
        public static final class Mod0 {
            public static final int driveMotorID = 6;
            public static final int angleMotorID = 4;
            public static final int canCoderID = 5;
            public static final Rotation2d angleOffset = Rotation2d.fromDegrees(129.023 - 180);
            public static final SwerveModuleConfig config =
                new SwerveModuleConfig(driveMotorID, angleMotorID, canCoderID, angleOffset);
        }

        // Front Right
        public static final class Mod1 {
            public static final int driveMotorID = 9;
            public static final int angleMotorID = 7;
            public static final int canCoderID = 8;
            public static final Rotation2d angleOffset = Rotation2d.fromDegrees(206.806);
            public static final SwerveModuleConfig config =
                new SwerveModuleConfig(driveMotorID, angleMotorID, canCoderID, angleOffset);
        }

        // Back Left
        public static final class Mod2 {
            public static final int driveMotorID = 3;
            public static final int angleMotorID = 1;
            public static final int canCoderID = 2;
            public static final Rotation2d angleOffset = Rotation2d.fromDegrees(101.426);
            public static final SwerveModuleConfig config =
                new SwerveModuleConfig(driveMotorID, angleMotorID, canCoderID, angleOffset);
        }

        // Back Right
        public static final class Mod3 {
            public static final int driveMotorID = 12;
            public static final int angleMotorID = 10;
            public static final int canCoderID = 11;
            public static final Rotation2d angleOffset = Rotation2d.fromDegrees(-4.131);
            public static final SwerveModuleConfig config =
                new SwerveModuleConfig(driveMotorID, angleMotorID, canCoderID, angleOffset);
        }
    }

    public class AutonConstants {

    }
}
