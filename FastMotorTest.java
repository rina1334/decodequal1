package org.firstinspires.ftc.teamcode; //whole code wont work without the package

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

@TeleOp(name = "claire2")
public final class FastMotorTest extends OpMode {
    private GamepadEx gp1; //making controller 1
    private Motor intakeMotor; //intake motor

    public void configureHardware() {
        //drivetrain configs
        gp1 = new GamepadEx(gamepad1); //setting the first controller to the gamepad
        intakeMotor = new Motor(hardwareMap, "intake_motor");
    }

    @Override
    public void init() { //when the program starts
        configureHardware(); //calls the method built on line 18
    }

    @Override
    public void loop() {
        intakeMotor.set(applyDeadzone(gp1.getLeftY()));

    }
    private double applyDeadzone(double value) { //elimates random strafing
        return Math.abs(value) > 0.05 ? value : 0.0;
    }
}
