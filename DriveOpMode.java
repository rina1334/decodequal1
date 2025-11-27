package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

@TeleOp(name = "qual1decode")
public final class DriveOpMode extends OpMode {

    private GamepadEx gp1, gp2;
    private MecanumDrive drive;
    private Motor frontLeft, frontRight, backLeft, backRight;
    private Motor intakeMotor, outtakeMotor;
    private Servo sortingServo, pushServo;

    private static final double POWER_REDUCTION = 0.5;
    private static final double SERVO_DOWN = 0.8;
    private static final double SERVO_UP = 0.45;

    // Burst indexing variables
    private boolean burstActive = false;
    private long burstStartTime = 0;
    private long lastBurstEnd = 0;
    private static final long BURST_DURATION = 300; // ms servo spins to move one ball
    private static final long BURST_COOLDOWN = 200; // ms before next burst
    private static final double BURST_SPEED = 0.32;  // slower than full speed

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
        //frontLeft.setInverted(true);
        //backLeft.setInverted(true);

        intakeMotor = new Motor(hardwareMap, "intake_motor");
        outtakeMotor = new Motor(hardwareMap, "outtake_motor");
        outtakeMotor.setInverted(true);

        sortingServo = hardwareMap.get(Servo.class, "sorting_servo");
        pushServo = hardwareMap.get(Servo.class, "push_servo");

        pushServo.setPosition(SERVO_DOWN);
        sortingServo.setPosition(0.5); // stop CR servo at start
    }

    @Override
    public void loop() {

        // --- Drivetrain ---
        double x = -applyDeadzone(gp1.getLeftX());
        double y = -applyDeadzone(gp1.getLeftY());
        double rx = -applyDeadzone(gp1.getRightX());

        double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);

        double frontLeftPower  = (y + x + rx) / denominator * POWER_REDUCTION;
        double backLeftPower   = (y - x + rx) / denominator * POWER_REDUCTION;
        double frontRightPower = (y - x - rx) / denominator * POWER_REDUCTION;
        double backRightPower  = (y + x - rx) / denominator * POWER_REDUCTION;

// --- Scale right side to fix left drift ---
        double rightCorrection = 0.97; // adjust until robot drives straight
        frontRightPower *= rightCorrection;
        backRightPower  *= rightCorrection;

        frontLeft.set(frontLeftPower);
        backLeft.set(backLeftPower);
        frontRight.set(frontRightPower);
        backRight.set(backRightPower);


        // --- Intake & Burst Indexing ---
        double intakeY = applyDeadzone(gp2.getLeftY());
        if (intakeY > 0.05) {
            intakeMotor.set(intakeY);

            // Start a burst if servo is not active and cooldown passed
            if (!burstActive && System.currentTimeMillis() - lastBurstEnd >= BURST_COOLDOWN) {
                sortingServo.setPosition(BURST_SPEED);
                burstStartTime = System.currentTimeMillis();
                burstActive = true;
            }
        } else if(intakeY < -0.05){
            intakeMotor.set(-intakeY);
        } else {
            intakeMotor.set(0);
        }

        // Manage burst timing
        if (burstActive) {
            if (System.currentTimeMillis() - burstStartTime >= BURST_DURATION) {
                sortingServo.setPosition(0.5); // stop CR servo
                burstActive = false;
                lastBurstEnd = System.currentTimeMillis();
            }
        }

        // --- Outtake ---
        double outtakeY = applyDeadzone(gp2.getRightY());
        outtakeMotor.set(outtakeY);

        // --- Push servo ---
        if (gamepad2.left_trigger > 0) pushServo.setPosition(SERVO_UP);
        if (gamepad2.right_trigger > 0) pushServo.setPosition(SERVO_DOWN);
    }

    private double applyDeadzone(double value) {
        return Math.abs(value) > 0.05 ? value : 0.0;
    }
}
