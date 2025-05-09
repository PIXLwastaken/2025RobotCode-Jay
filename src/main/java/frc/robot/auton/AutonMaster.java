package frc.robot.auton;

import static edu.wpi.first.units.Units.Rotation;

import com.fasterxml.jackson.databind.util.Named;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.config.PIDConstants;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;
import com.pathplanner.lib.path.PathConstraints;
import com.pathplanner.lib.util.PathPlannerLogging;

import edu.wpi.first.math.estimator.PoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.ConditionalCommand;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.ParallelRaceGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import frc.robot.algaeflywheel.AlgaeRoller;
import frc.robot.algaepivot.AlgaeSubsystem;
import frc.robot.algaepivot.commands.SetAngleAlgaePivot;
import frc.robot.commands.RobotCommands;
import frc.robot.driver.DriverXbox;
import frc.robot.drivetrain.CommandSwerveDrivetrain;
import frc.robot.elevator.ElevatorSubsystem;
import frc.robot.elevator.commands.SetElevatorState;
import frc.robot.rushinator.RushinatorPivot;
import frc.robot.rushinator.RushinatorWrist;
import frc.robot.rushinator.commands.SetArmState;
import frc.robot.rushinator.commands.SetRollersVoltage;
import frc.robot.rushinator.commands.SetWristState;
import frc.robot.vision.LineupMaster;
import frc.robot.vision.PoseEstimatorSubsystem;
import frc.robot.vision.VisionConfig.HPStation;
import frc.robot.vision.commands.AutoAlign;
import frc.robot.vision.commands.AutoAlignHP;

/* MASTER AUTON CLASS */
public class AutonMaster {
    // private static Field2d mGameField;
    private static SendableChooser<Command> autonChooser = new SendableChooser<>();
    private static CommandSwerveDrivetrain drivetrain;

    private static AutonMaster mInstance;

    
    public AutonMaster() {
        drivetrain = CommandSwerveDrivetrain.getInstance();
        try {
            var config = RobotConfig.fromGUISettings();
            AutoBuilder.configure(
                () -> {
                    Pose2d poseEstPose = PoseEstimatorSubsystem.getInstance().getCurrentPose();
                    return poseEstPose != Pose2d.kZero ? poseEstPose : CommandSwerveDrivetrain.getInstance().getPose();
                },   // Supplier of current robot pose
                drivetrain::resetPose,         // Consumer for seeding pose against auto
                drivetrain::getRobotRelvativeSpeeds, // Supplier of current robot speeds
                // Consumer of ChassisSpeeds and feedforwards to drive the robot
                (speeds, feedforwards) -> drivetrain.setControl(
                    drivetrain.m_pathApplyRobotSpeeds.withSpeeds(speeds)
                        .withWheelForceFeedforwardsX(feedforwards.robotRelativeForcesXNewtons())
                        .withWheelForceFeedforwardsY(feedforwards.robotRelativeForcesYNewtons())
                ),
                new PPHolonomicDriveController(
                    // PID constants for translation
                    new PIDConstants(4.5, 0.0, 0.0),
                    // PID constants for rotation
                    new PIDConstants(5.5, 0, 0.0)
                ),
                config,
                // Assume the path needs to be flipped for Red vs Blue, this is normally the case
                () -> DriverStation.getAlliance().orElse(Alliance.Blue) == Alliance.Red,
                drivetrain // Subsystem for requirements
            );
        } catch (Exception ex) {
            DriverStation.reportError("Failed to load PathPlanner config and configure AutoBuilder", ex.getStackTrace());
        }

        configureNamedCommands();
        configurePathPlannerLogging();
        
        //Add Auton Options Here
        // autonChooser.addOption("TestPath", AutoBuilder.buildAuto("TestAuto"));
        // autonChooser.addOption("LeftStart3PieceCoralFeed", AutoBuilder.buildAuto("L-3C-F"));
        autonChooser.addOption("RightStart3PieceCoralFeed", AutoBuilder.buildAuto("Right_3.5_FDC"));
        autonChooser.addOption("LeftStart3PieceCoralFeed", AutoBuilder.buildAuto("Left_3.5_IKL"));
        autonChooser.addOption("MidStart1PieceCoral", AutoBuilder.buildAuto("Mid_1_G"));
        autonChooser.addOption("RightMoveSomeone", AutoBuilder.buildAuto("Right_Move_Someone"));
        autonChooser.addOption("MidStartAlgaeAuto", AutoBuilder.buildAuto("Mid_1AL_G"));
        autonChooser.addOption("RightStartRightLoli3.5Piece", AutoBuilder.buildAuto("Right_3.5_CBA_Ground"));
        autonChooser.addOption("LeftStartLeftLoli3.5Piece", AutoBuilder.buildAuto("Left_3.5_LAB_Ground"));
        autonChooser.addOption("MidLeftStartLeftLoli3.5Piece", AutoBuilder.buildAuto("Mid_3.5_GAB_Ground"));
        autonChooser.addOption("MidRightStartLeftLoli3.5Piece", AutoBuilder.buildAuto("Mid_3.5_GBA_Ground"));
    }
    

    public static AutonMaster getInstance() {
        if (mInstance == null) {
            mInstance = new AutonMaster();
        }
        return mInstance;
    }

    public void configureNamedCommands() {
        NamedCommands.registerCommand("PrimeScoreL4", RobotCommands.primeScoreCoralAutonL4());

        NamedCommands.registerCommand("LineUpLeft", new AutoAlign(() -> LineupMaster.getClosestReefFace(()-> PoseEstimatorSubsystem.getInstance().getCurrentPose()).leftBranch, () -> true));
        NamedCommands.registerCommand("LineUpRight", new AutoAlign(() -> LineupMaster.getClosestReefFace(()-> PoseEstimatorSubsystem.getInstance().getCurrentPose()).rightBranch, () -> false));

        // NamedCommands.registerCommand("LineUpLeft", new LineupMaster().directDriveToNearestLeftBranch());
        // NamedCommands.registerCommand("LineUpRight", new LineupMaster().directDriveToNearestRightBranch());


        NamedCommands.registerCommand("AutonScoreL1", RobotCommands.scoreCoralAutonL1());
        NamedCommands.registerCommand("AutonScoreL2", RobotCommands.scoreCoralAutonL2());
        NamedCommands.registerCommand("AutonScoreL3", RobotCommands.scoreCoralAutonL3());
        NamedCommands.registerCommand("AutonScoreL4", RobotCommands.scoreCoralAutonL4());
        NamedCommands.registerCommand("HPPickup", RobotCommands.autoHPPickUp());
        NamedCommands.registerCommand("StopRollers", new SetRollersVoltage(0.0));
        NamedCommands.registerCommand("PreHPPickUp", new ParallelCommandGroup(new SetWristState(RushinatorWrist.State.kHPMid), new SetArmState(RushinatorPivot.State.kHPIntake), new SetRollersVoltage(4.5)));
        NamedCommands.registerCommand("LolipopRight", new ParallelRaceGroup(
            RobotCommands.coralPrimeShoot(RushinatorPivot.State.kLoliPop, RushinatorWrist.State.kLoliLeft),
            new SetRollersVoltage(4.0)));
        NamedCommands.registerCommand("LolipopLeft", new ParallelRaceGroup(
            RobotCommands.coralPrimeShoot(RushinatorPivot.State.kLoliPop, RushinatorWrist.State.kLoliRight),
            new SetRollersVoltage(4.0)));
        NamedCommands.registerCommand("ResetArmWrist", new ParallelRaceGroup(
            RobotCommands.coralPrimeShoot(RushinatorPivot.State.kStowTravel, RushinatorWrist.State.kTravelRight),
            new SetRollersVoltage(0)));
        NamedCommands.registerCommand("AlgaeIntakeL2", new ParallelCommandGroup(
            new SetAngleAlgaePivot(AlgaeSubsystem.State.kReefIntake),
            new SetElevatorState(ElevatorSubsystem.State.kAlgaeL2),
            new AlgaeRoller.IntakeCommand()
        ));
        NamedCommands.registerCommand("AlgaeIntakeL3", new ParallelCommandGroup(
            new SetAngleAlgaePivot(AlgaeSubsystem.State.kReefIntake),
            new SetElevatorState(ElevatorSubsystem.State.kAlgaeL3),
            new AlgaeRoller.IntakeCommand()
        ));
        NamedCommands.registerCommand("AlgaeConstantIntake", new AlgaeRoller.SetIndexerVoltagCommand(AlgaeRoller.getInstance(), -5));

        NamedCommands.registerCommand("ZeroAlgae", RobotCommands.algaePrime(AlgaeSubsystem.State.kStow, ElevatorSubsystem.State.kZero));
        NamedCommands.registerCommand("AlgaeBargePrime", new ParallelCommandGroup(
            RobotCommands.algaePrime(AlgaeSubsystem.State.kStow, ElevatorSubsystem.State.kCoralL4),
            new AlgaeRoller.PrimeCommand()));
        NamedCommands.registerCommand("ScoreAlgaeBarge", new AlgaeRoller.ShootCommand());

        // NamedCommands.registerCommand("LineUpBlueHPStationRight", new AutoAlignHP(() -> HPStation.BLU_RIGHT_STATION.AprilTag));
        // NamedCommands.registerCommand("LineUpBlueHPStationLeft", new AutoAlignHP(() -> HPStation.BLU_LEFT_STATION.AprilTag));
        // NamedCommands.registerCommand("LineUpRedHPStationRight", new AutoAlignHP(() -> HPStation.RED_RIGHT_STATION.AprilTag));
        // NamedCommands.registerCommand("LineUpRedHPStationLeft", new AutoAlignHP(() -> HPStation.RED_LEFT_STATION.AprilTag));

        NamedCommands.registerCommand("LineUpHPStationRight", new ConditionalCommand(
            new AutoAlignHP(() -> HPStation.RED_RIGHT_STATION.AprilTag), new AutoAlignHP(() -> HPStation.BLU_RIGHT_STATION.AprilTag), 
            () -> DriverStation.getAlliance().get() == Alliance.Red));
        
        NamedCommands.registerCommand("LineUpHPStationLeft", new ConditionalCommand(
            new AutoAlignHP(() -> HPStation.RED_LEFT_STATION.AprilTag), new AutoAlignHP(() -> HPStation.BLU_LEFT_STATION.AprilTag), 
            () -> DriverStation.getAlliance().get() == Alliance.Red));

    }

    public Command getTestPathFindingCommand() {
        PathConstraints pathFindConstraints = new PathConstraints(3.00, 3.00, Units.degreesToRadians(540), Units.degreesToRadians(720));
        Translation2d pathFindGoalPoseTranslation = new Translation2d(1.202, 2.169);
        Rotation2d pathFindGoalPoseRotation = Rotation2d.fromDegrees(0);
        Pose2d pathFindGoalPose = new Pose2d(pathFindGoalPoseTranslation, pathFindGoalPoseRotation);
        return AutoBuilder.pathfindToPose(pathFindGoalPose, pathFindConstraints);
    }

    public SendableChooser<Command> getAutonSelector() {
        return autonChooser;
    }

    private void configurePathPlannerLogging() {
        // mGameField = new Field2d();
        // SmartDashboard.putData("Field", mGameField);

        // PathPlannerLogging.setLogCurrentPoseCallback((pose) -> {
        //     mGameField.setRobotPose(pose);
        // });

        // PathPlannerLogging.setLogTargetPoseCallback((pose) -> {
        //     mGameField.getObject("target pose").setPose(pose);
        // });

        // PathPlannerLogging.setLogActivePathCallback((poses) -> {
        //     mGameField.getObject("path").setPoses(poses);
        // });
    }
}