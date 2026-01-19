package org.valkyrienskies.clockwork.util

object ClockworkConstants {
    object Nbt {
        val SELECTED_DATA: String = "SelectedData"
        val RIGHT_RULES: String = "RightRules"
        val LEFT_RULES: String = "LeftRules"
        val BACKWARD_RULES: String = "BackwardRules"
        val FORWARD_RULES: String = "ForwardRules"
        val CHANGE_TIMER: String = "ChangeTimer"
        val REDSTONE_LEVEL: String = "RedstoneLevel"

        @JvmField
        var FAN_ID: String = "FanId"

        @JvmField
        var ALREADY_ADDED: String = "AlreadyAdded"
        val OFFSET: String = "Offset"
        val FACING: String = "Facing"
        val SAILS: String = "Sails"
        val INVERTED: String = "Inverted"
        val ID: String = "Id"
        val ROT_SPEED: String = "RotSpeed"
        val TRIGGER_HEIGHT: String = "TriggerHeight"
        val TRIGGER_SENSITIVITY: String = "TriggerSensitivity"
        val TRIGGER_DIRECTION: String = "TriggerDirection"
        val SHIPTRAPTION_ID: String = "ShiptraptionID"
        val BEARING_ID: String = "BearingID"
        val OPEN: String = "Open"
        val ANGLE: String = "Angle"
        val RUNNING: String = "Running"
        val DISASSEMBLING: String = "Disassembling"
        val ASSEMBLING: String = "Assembling"
        val IS_ASSEMBLED: String = "IsAssembled"
        val IDLE_PROGRESS: String = "IdleProgress"
        val DISASSEMBLY_PROGRESS: String = "DisassemblyProgress"
        val ASSEMBLY_PROGRESS: String = "AssemblyProgress"
        val ANIMATION_STATE: String = "AnimationState"
        val SHIP_STUCK: String = "ShipStuck"
        val ATTACHED_SHIP: String = "AttachedShip"
        val SHIP_STICKER_ALREADY_POWERED: String = "ShipStickerAlreadyPowered"
        val CONDENSED_DATA: String = "CondensedData"
        val SHIP_SLICKER_DISTANCE: String = "ShipStickerDistance"
        val ATTACHMENT_CONSTRAINT: String = "AttachmentConstraint"
        val ATTACHMENT_CONSTRAINT_ID: String = "AttachmentConstraintId"
        val ORIENTATION_CONSTRAINT: String = "OrientationConstraint"
        val ORIENTATION_CONSTRAINT_ID: String = "OrientationConstraintId"
        val ORIGINAL_DIRECTION: String = "OriginalDirection"
        val MANUAL_TARGET_ANGLE_CHANGE: String = "ManualTargetAngleChange"
        val DATA: String = "Data"
        val OLD_POS: String = "OldPos"
        val OLD_SHIPTRAPTION_CENTER: String = "OldShiptraptionCenter"
        val NEW_SHIPTRAPTION_CENTER: String = "NewShiptraptionCenter"
        val SEQUENCED_ANGLE_LIMIT: String = "SequencedAngleLimit"
        val SEQUENCED_ANGLE_PROGRESS: String = "SequencedAngleProgress"
    }

    object Misc {
        const val DUCT_RADIUS = 0.1875
        const val DUCT_AREA = 0.11045
    }
}