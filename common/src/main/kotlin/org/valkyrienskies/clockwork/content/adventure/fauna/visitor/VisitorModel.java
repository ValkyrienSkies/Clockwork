// Made with Blockbench 5.0.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class wanderer<T extends WandererEntity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "wanderer"), "main");
	private final ModelPart right_leg;
	private final ModelPart left_leg;
	private final ModelPart left_arm;
	private final ModelPart right_arm;
	private final ModelPart body;
	private final ModelPart upper_body;
	private final ModelPart core;
	private final ModelPart head;
	private final ModelPart pupil;
	private final ModelPart lid;
	private final ModelPart flower;

	public wanderer(ModelPart root) {
		this.right_leg = root.getChild("right_leg");
		this.left_leg = root.getChild("left_leg");
		this.left_arm = root.getChild("left_arm");
		this.right_arm = root.getChild("right_arm");
		this.body = root.getChild("body");
		this.upper_body = this.body.getChild("upper_body");
		this.core = this.upper_body.getChild("core");
		this.head = root.getChild("head");
		this.pupil = this.head.getChild("pupil");
		this.lid = this.head.getChild("lid");
		this.flower = this.head.getChild("flower");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 28).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 33.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(24, 28).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 33.0F, 2.0F, new CubeDeformation(0.1F)), PartPose.offset(-2.0F, -8.0F, 0.0F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 15).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 33.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(8, 28).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 33.0F, 2.0F, new CubeDeformation(0.1F)), PartPose.offset(2.0F, -8.0F, 0.0F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 28).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 29.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(40, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 29.0F, 2.0F, new CubeDeformation(0.1F)), PartPose.offset(5.0F, -15.0F, 0.0F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 31).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 29.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(48, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 29.0F, 2.0F, new CubeDeformation(0.1F)), PartPose.offset(-5.0F, -15.0F, 0.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(48, 55).addBox(-3.5F, 4.1667F, -2.5F, 7.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(72, 55).addBox(-3.5F, 7.1667F, -2.5F, 7.0F, 3.0F, 5.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, -14.1667F, 0.0F));

		PartDefinition upper_body = body.addOrReplaceChild("upper_body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -3.2881F, 8.0F, 8.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(8, 15).addBox(-4.0F, -10.0F, -3.2881F, 8.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.1667F, -0.2119F));

		PartDefinition cube_r1 = upper_body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(30, 9).addBox(-1.5084F, -1.5F, -1.6248F, 0.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.25F, 0.0F, -3.7119F, 0.0F, 0.3927F, 0.0F));

		PartDefinition cube_r2 = upper_body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(30, 4).addBox(1.5084F, -1.5F, -1.6248F, 0.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.25F, 0.0F, -3.7119F, 0.0F, -0.3927F, 0.0F));

		PartDefinition core = upper_body.addOrReplaceChild("core", CubeListBuilder.create().texOffs(30, 0).addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -3.7881F));

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(48, 31).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -22.0F, 0.0F));

		PartDefinition pupil = head.addOrReplaceChild("pupil", CubeListBuilder.create().texOffs(56, 0).addBox(-0.5F, -1.5F, 0.0F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 0.25F, -3.05F));

		PartDefinition lid = head.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(48, 43).addBox(-5.0F, -18.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.1F)), PartPose.offset(2.0F, 15.0F, 0.0F));

		PartDefinition flower = head.addOrReplaceChild("flower", CubeListBuilder.create().texOffs(8, 15).addBox(-1.0F, -2.0F, 0.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -2.25F, -4.5F, -0.3927F, 0.3927F, 0.0F));

		PartDefinition cube_r3 = flower.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.75F, -1.0F, 1.5F, -0.1231F, -0.2316F, 0.4943F));

		PartDefinition cube_r4 = flower.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.75F, -1.0F, 1.5F, 0.0F, 0.2618F, 0.0F));

		PartDefinition cube_r5 = flower.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(16, 15).addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, -1.0F, 1.25F, 0.0F, 0.0F, 0.3927F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(WandererEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}