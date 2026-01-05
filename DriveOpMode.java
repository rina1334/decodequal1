package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

@TeleOp(name = "qual1decode")
public final class DriveOpMode extends OpMode {

    private GamepadEx gp1, gp2;
    private MecanumDrive drive;
    private Motor frontLeft, frontRight, backLeft, backRight;
    private Motor intakeMotor, outtakeMotor;
    private Servo pushServo;
    private Servo sortingServo;

    private static final double POWER_REDUCTION = 0.4;
    private static final double SERVO_DOWN = 1.0;
    private static final double SERVO_UP = 0.6;

    private ElapsedTime pushTimer = new ElapsedTime();

    private static final double SHOOTING_1 = 0.024;
    private static final double SHOOTING_2 = 0.094;
    private static final double SHOOTING_3 = 0.164;
    private static final double INTAKE_1 = 0.06;
    private static final double INTAKE_2 = 0.13;
    private static final double INTAKE_3 = 0.2;


    private static final double SERVO_STEP = 0.07;       // amount to move each step
    private static final double SERVO_MIN = 0.0;         // min position
    private static final double SERVO_MAX = 1.0;         // max position
    private static final long SERVO_DELAY_MS = 300;      // delay between steps

    private boolean servoIncreasing = true;              // direction flag
    private ElapsedTime servoStepTimer = new ElapsedTime(); // timer to manage steps


    @Override
    public void init() {
        gp1 = new GamepadEx(gamepad1);
        gp2 = new GamepadEx(gamepad2);

        frontLeft = new Motor(hardwareMap, "leftFront");
        frontRight = new Motor(hardwareMap, "rightFront");
        backLeft = new Motor(hardwareMap, "leftBack");
        backRight = new Motor(hardwareMap, "rightBack");

        drive = new MecanumDrive(frontLeft, frontRight, backLeft, backRight);
        frontRight.setInverted(true);
        backRight.setInverted(true);

        intakeMotor = new Motor(hardwareMap, "intake_motor");
        outtakeMotor = new Motor(hardwareMap, "outtake_motor");
        outtakeMotor.setInverted(true);

        sortingServo = hardwareMap.get(Servo.class, "sorting_servo");
        pushServo = hardwareMap.get(Servo.class, "push_servo");

        pushServo.setPosition(1.0);
        //sortingServo.setPosition(0);
    }

    @Override
    public void loop() {

        // --- Drivetrain ---
        double x = -applyDeadzone(gp1.getLeftX());
        double y = -applyDeadzone(gp1.getLeftY());
        double rx = -applyDeadzone(gp1.getRightX());
        double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);

        double frontLeftPower  = (y + x + rx) / denominator * POWER_REDUCTION;
        double backLeftPower   = ((y - x + rx) / denominator) * POWER_REDUCTION * 0.97;
        double frontRightPower = (y - x - rx) / denominator * POWER_REDUCTION;
        double backRightPower  = (y + x - rx) / denominator * POWER_REDUCTION;
        double rightCorrection = 0.935;

        frontRightPower *= rightCorrection;
        backRightPower  *= rightCorrection;

        frontLeft.set(frontLeftPower);
        backLeft.set(backLeftPower);
        frontRight.set(frontRightPower);
        backRight.set(backRightPower);



        // --- Sorting servo ---
        if (gamepad2.x){
            sortingServo.setPosition(SHOOTING_1);
        } else if(gamepad2.y) {
            sortingServo.setPosition(SHOOTING_2);
        } else if(gamepad2.b) {
            sortingServo.setPosition(SHOOTING_3);
        }


        double intakePower = applyDeadzone(gp2.getLeftY());
        intakeMotor.set(intakePower);

        if (Math.abs(intakePower) > 0) { // only move servo if intake is running
            if (servoStepTimer.milliseconds() > SERVO_DELAY_MS) {
                double pos = sortingServo.getPosition();

                if (servoIncreasing) {
                    pos += SERVO_STEP;
                    if (pos >= SERVO_MAX) {
                        pos = SERVO_MAX;
                        servoIncreasing = false; // switch direction
                    }
                } else {
                    pos -= SERVO_STEP;
                    if (pos <= SERVO_MIN) {
                        pos = SERVO_MIN;
                        servoIncreasing = true; // switch direction
                    }
                }

                sortingServo.setPosition(pos);
                servoStepTimer.reset();
            }
        }




        // --- Outtake motor ---
        //double outtakeY = applyDeadzone(gp2.getRightY());
        if (gamepad2.right_bumper){
            outtakeMotor.set(0.95);
        }
        if (gamepad2.left_bumper) {
            outtakeMotor.set(0.0);
        }




        // --- Push servo ---
        if (gamepad2.left_trigger > 0) {
            pushServo.setPosition(SERVO_UP);
            pushTimer.reset();
        }
        if (pushTimer.milliseconds() > 250) pushServo.setPosition(SERVO_DOWN);
        if (gamepad2.right_trigger > 0) pushServo.setPosition(SERVO_DOWN);
    }


    private double applyDeadzone(double value) {
        return Math.abs(value) > 0.6 ? value : 0.0;
    }
}
