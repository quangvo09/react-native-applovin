#if __has_include(<React/RCTBridgeModule.h>)
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#else
#import "RCTBridgeModule.h"
#import "RCTEventEmitter.h"
#endif

#import <AppLovinSDK/AppLovinSDK.h>

@property (nonatomic, strong) ALAd *ad;
@end

@interface RNAppLovinInterstitial : RCTEventEmitter <RCTBridgeModule>
@end
