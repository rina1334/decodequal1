package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
//import com.seattlesolvers.solverslib.hardware.motors.CRServo;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

@TeleOp(name = "qual1decode")
public final class DriveOpMode extends OpMode {

    private GamepadEx gp1, gp2;
    private MecanumDrive drive;
    private Motor frontLeft, frontRight, backLeft, backRight;
    private Motor intakeMotor, outtakeMotor;
    private Servo pushServo;
    private CRServo sortingServo;

    private static final double POWER_REDUCTION = 0.4;

    private static final double SERVO_DOWN = 0.8;
    private static final double SERVO_UP = 0.45;

    // Burst indexing variables
    private boolean burstActive = false;
    private long burstStartTime = 0;
    private long lastBurstEnd = 0;
    private static final long BURST_DURATION = 300; // ms servo spins to move one ball
    private static final long BURST_COOLDOWN = 200; // ms before next burst
    private static final double BURST_SPEED = 1;  // full speed

    //position
    private int currentIndex = 0;  // 0 = 0°, 1 = 120°, 2 = 240°
    private final long sectorTime = 200;  // adjust after tuning
    private boolean moving = false;
    private long moveStart = 0;
    private int targetIndex = 0;
    private boolean xPresedLast = false;
    private boolean yPressedLast = false;
    private boolean aPressedLast = false;
    private boolean bPressedLast = false;
    private long moveStartTime = 0;
    private long moveDuration = 0;

    ElapsedTime timer = new ElapsedTime();
    boolean timedRun = false;


    private void selectPosition(int target) {
        if (moving) return;  // ignore if already moving

        targetIndex = target;

        // compute difference in sectors (0,1,2)
        int diff = targetIndex - currentIndex;

        // normalize to range 0..2
        diff = (diff + 3) % 3;

        // each sector = 120° = 200 ms
        moveDuration = diff * sectorTime;

        if (moveDuration == 0) {
            // already in position
            return;
        }

        // direction: only spin forward
        sortingServo.setPower(1.0);

        moving = true;
        moveStart = System.currentTimeMillis();
    }


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
        frontLeft.setInverted(true);

        intakeMotor = new Motor(hardwareMap, "intake_motor");
        outtakeMotor = new Motor(hardwareMap, "outtake_motor");
        outtakeMotor.setInverted(true);

        sortingServo = hardwareMap.get(CRServo.class, "sorting_servo");
        pushServo = hardwareMap.get(Servo.class, "push_servo");

        pushServo.setPosition(SERVO_DOWN);
        sortingServo.setPower(0); // stop CR servo at start
    }

    @Override
    public void loop() {

        // --- Drivetrain ---
        double x = -applyDeadzone(gp1.getLeftX());
        double y = -applyDeadzone(gp1.getLeftY());
        double rx = -applyDeadzone(gp1.getRightX());

        double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);

        double frontLeftPower  = (y + x + rx) / denominator * POWER_REDUCTION;
        double backLeftPower  = ((y - x + rx) / denominator) * POWER_REDUCTION * 0.97; // tweak 0.95–1.0
        double frontRightPower = (y - x - rx) / denominator * POWER_REDUCTION;
        double backRightPower  = (y + x - rx) / denominator * POWER_REDUCTION;

//        // Scale right side to fix left drift
        double rightCorrection = 0.935; // adjust until robot drives straight
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
                sortingServo.setPower(BURST_SPEED);
                burstStartTime = System.currentTimeMillis();
                burstActive = true;
            }
        } else if(intakeY < -0.05){
            intakeMotor.set(intakeY);
        }

        // Manage burst timing
        if (burstActive) {
            if (System.currentTimeMillis() - burstStartTime >= BURST_DURATION) {
                sortingServo.setPower(0); // stop CR servo
                burstActive = false;
                lastBurstEnd = System.currentTimeMillis();
            }
        }

        // ---- BUTTONS FOR POSITIONS ---- //
        if (gamepad2.a && !aPressedLast) selectPosition(0);  // 0°
        aPressedLast = gamepad2.a;

        if (gamepad2.b && !bPressedLast) selectPosition(1);  // 120°
        bPressedLast = gamepad2.b;

        if (gamepad2.y && !yPressedLast) selectPosition(2);  // 240°
        yPressedLast = gamepad2.y;

        if ((gamepad1.a || gamepad1.b || gamepad1.y) && !timedRun) {
            intakeMotor.set(1.0);
            timer.reset();
            timedRun = true;
        }

        if (timedRun && timer.seconds() > 1.0) {  // 1 second
            intakeMotor.set(0);
            timedRun = false;
        }



// ---- MOVEMENT UPDATE ---- //
        if (moving) {
            if (System.currentTimeMillis() - moveStart >= moveDuration) {
                sortingServo.setPower(0);  // stop
                moving = false;
                currentIndex = targetIndex;
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
