package frc.robot.rushinator;

import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.MagnetSensorConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.signals.SensorDirectionValue;

import edu.wpi.first.math.controller.ArmFeedforward;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.algaepivot.AlgaeSubsystem;

public class RushinatorPivot extends SubsystemBase {
    public static class Settings {
        static final int kTalonPivotID = 14;
        static final int kCANcoderPivotID = 25;

        static final double kG = 0.19; // V
        static final double kS = 0.0; // V / rad
        static final double kV = 0; // V * sec / rad
        static final double kA = 0; // V * sec^2 / rad

        static final Rotation2d kMaxVelocity = Rotation2d.fromDegrees(300);
        static final Rotation2d kMaxAcceleration = Rotation2d.fromDegrees(600);
        static final double kP = 15.0;
        static final double kI = 0.0;
        static final double kD = 0;

        static final double kZeroOffset = 0.3225; // rotations

        // TODO: Enable lower min-pos to bring down CoG when elevator is up. We should be able to tuck the shooter into the elevator.
        static final Rotation2d kMinPos = Rotation2d.fromRotations(-0.04);
        static final Rotation2d kMaxPos = Rotation2d.fromRotations(0.23);
    }

    public enum State {
        kFloorIntake(Settings.kMinPos),
        kHPIntake(Rotation2d.fromRotations(0.06)),
        kScoreL1(Rotation2d.fromRotations(0)),
        kScoreL2(Rotation2d.fromRotations(0.15)),
        kScoreL3(Rotation2d.fromRotations(0.15)),
        kScoreL4(Rotation2d.fromRotations(0.15)),
        kStow(Rotation2d.fromRotations(0.18)),
        kTuck(Settings.kMaxPos);

        State(Rotation2d pos) {
            this.pos = pos;
        }
        public final Rotation2d pos;
    }

    private final TalonFX mTalonPivot;
    private final CANcoder mCANcoderPivot;
    private final ArmFeedforward mFFController;
    private final ProfiledPIDController mPPIDController;

    public static State kLastState;

    private RushinatorPivot() {
        mTalonPivot = new TalonFX(Settings.kTalonPivotID);
        mTalonPivot.getConfigurator().apply(new TalonFXConfiguration().withMotorOutput(new MotorOutputConfigs()
                .withInverted(InvertedValue.Clockwise_Positive)
                .withNeutralMode(NeutralModeValue.Coast)
        ));

        mCANcoderPivot = new CANcoder(Settings.kCANcoderPivotID);
        mCANcoderPivot.getConfigurator().apply(new CANcoderConfiguration().withMagnetSensor(new MagnetSensorConfigs().
                withSensorDirection(SensorDirectionValue.Clockwise_Positive).
                withMagnetOffset(Settings.kZeroOffset)
        ));

        mFFController = new ArmFeedforward(Settings.kS, Settings.kG, Settings.kV, Settings.kA);
        mPPIDController = new ProfiledPIDController(Settings.kP, Settings.kI, Settings.kD, new TrapezoidProfile.Constraints(
                Settings.kMaxVelocity.getRadians(),
                Settings.kMaxAcceleration.getRadians()
        ));

        setTargetState(State.kTuck);
    }


    private static RushinatorPivot mInstance;
    public static RushinatorPivot getInstance() {
        if (mInstance == null) {
            mInstance = new RushinatorPivot();
        }
        return mInstance;
    }

    public void setTargetState(State targetState) {
        kLastState = targetState;
        setTargetPosition(targetState.pos);
    }

    public void setTargetPosition(Rotation2d targetPosition) {
        // NOTE: Use radians for target goal to align with re:calc constant units
        mPPIDController.setGoal(targetPosition.getRadians());
    }

    public Rotation2d getArmPosition() {
        var pos = mCANcoderPivot.getAbsolutePosition().getValueAsDouble();
        return Rotation2d.fromRotations(pos);
    }

    public Rotation2d getArmVelocity() {
        var vel = mCANcoderPivot.getVelocity().getValueAsDouble();
        return Rotation2d.fromRotations(vel);
    }

    @Override
    public void periodic() {
        var voltage = mPPIDController.calculate(getArmPosition().getRadians());
        voltage += mFFController.calculate(getArmPosition().getRadians(), mPPIDController.getSetpoint().velocity);
        mTalonPivot.setVoltage(voltage);

        // Telemetry
        SmartDashboard.putNumber("Algae Pivot Pos (rotations)", getArmPosition().getRotations());
        SmartDashboard.putNumber("Algae Pivot Target Pos (rotations)", Rotation2d.fromRadians(mPPIDController.getSetpoint().position).getRotations());
        SmartDashboard.putNumber("Algae Pivot Vel (rotations / sec)", getArmVelocity().getRotations());
        SmartDashboard.putNumber("Algae Pivot Target Vel (rotations / sec)", Rotation2d.fromRadians(mPPIDController.getSetpoint().velocity).getRotations());
        SmartDashboard.putNumber("Algae Pivot Applied Voltage", voltage);
    }

    public static class DefaultCommand extends Command {

        public DefaultCommand() {
            addRequirements(RushinatorPivot.getInstance());
        }

        @Override
        public void execute() {
            if (kLastState == State.kTuck) {
                RushinatorPivot.getInstance().setTargetState(State.kTuck);
            } else {
                RushinatorPivot.getInstance().setTargetState(State.kStow);
            }
        }

    }
}
