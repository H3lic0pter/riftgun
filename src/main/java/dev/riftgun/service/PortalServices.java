package dev.riftgun.service;

public final class PortalServices {
    public static DestinationDimensionPolicy DIMENSION_POLICY = new AvailableDimensionPolicy();
    public static DestinationSafetyInspector SAFETY_INSPECTOR = new VanillaDestinationSafetyInspector();
    public static SafeDestinationResolver SAFE_DESTINATION_RESOLVER = SafeDestinationResolver.IDENTITY;
    public static PortalEntityEligibilityPolicy ENTITY_ELIGIBILITY = new DefaultPortalEntityEligibilityPolicy();
    public static PortalClosePolicy CLOSE_POLICY = new FixedOpenDurationClosePolicy();
    public static PortalPlacementCapabilities PLACEMENT_CAPABILITIES = PortalPlacementCapabilities.DEFAULT;
    public static PortalMotionHistory MOTION_HISTORY = new ServerPortalMotionHistory();
    public static PortalMotionPredictor MOTION_PREDICTOR = new VanillaPortalMotionPredictor();
    public static PortalPlacementResolver PLACEMENT_RESOLVER = new VanillaPortalPlacementResolver();

    public static void bootstrap() {
        if (PortalGunLocator.LOCATORS.isEmpty()) {
            PortalGunLocator.register(new VanillaInventoryPortalGunLocator());
        }
    }

    private PortalServices() {}
}
