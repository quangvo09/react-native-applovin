#import "RNAppLovinInterstitial.h"

#if __has_include(<React/RCTUtils.h>)
#import <React/RCTUtils.h>
#else
#import "RCTUtils.h"
#endif

static NSString *const kEventAdLoaded = @"interstitialAdLoaded";
static NSString *const kEventAdFailedToLoad = @"interstitialAdFailedToLoad";
static NSString *const kEventAdOpened = @"interstitialAdOpened";
static NSString *const kEventAdFailedToOpen = @"interstitialAdFailedToOpen";
static NSString *const kEventAdClosed = @"interstitialAdClosed";
static NSString *const kEventAdLeftApplication = @"interstitialAdLeftApplication";

@implementation RNAppLovinInterstitial
{
    AdColonyInterstitial *_interstitial;
    NSArray *_testDevices;
    RCTPromiseResolveBlock _requestAdResolve;
    RCTPromiseRejectBlock _requestAdReject;
    BOOL hasListeners;
    BOOL isReady;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}

RCT_EXPORT_MODULE();

- (NSArray<NSString *> *)supportedEvents
{
    return @[
             kEventAdLoaded,
             kEventAdFailedToLoad,
             kEventAdOpened,
             kEventAdFailedToOpen,
             kEventAdClosed,
             kEventAdLeftApplication ];
}

#pragma mark exported methods

RCT_EXPORT_METHOD(setAdUnitID:(NSString *)adUnitID)
{
    _adUnitID = adUnitID;
}

RCT_EXPORT_METHOD(requestAd:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    isReady = false;
    _requestAdResolve = resolve;
    _requestAdReject = reject;

    [[ALSdk shared].adService loadNextAd: [ALAdSize sizeInterstitial] andNotify: self];

    [ALInterstitialAd shared].adDisplayDelegate = self;
    [ALInterstitialAd shared].adVideoPlaybackDelegate = self;
}

RCT_EXPORT_METHOD(showAd:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    if (isReady) {
      [[ALInterstitialAd shared] showOver: [UIApplication sharedApplication].keyWindow andRender: self.ad];
      resolve(nil);
    }
    else {
        reject(@"E_AD_NOT_READY", @"Ad is not ready.", nil);
    }
}

RCT_EXPORT_METHOD(isReady:(RCTResponseSenderBlock)callback)
{
    callback(@[[NSNumber numberWithBool:isReady]]);
}

- (void)startObserving
{
    hasListeners = YES;
}

- (void)stopObserving
{
    hasListeners = NO;
}

#pragma mark - Ad Load Delegate

- (void)adService:(nonnull ALAdService *)adService didLoadAd:(nonnull ALAd *)ad
{
    self.ad = ad;
    isReady = true;
    if (hasListeners) {
        [self sendEventWithName:kEventAdLoaded body:nil];
    }
    _requestAdResolve(nil);
}

- (void)adService:(nonnull ALAdService *)adService didFailToLoadAdWithError:(int)code
{
  NSString *errorReason = [NSString stringWithFormat:@"Error code: %d", code];
  NSError *error = [[NSError alloc] initWithDomain:@"" code:code userInfo:@{ NSLocalizedFailureReasonErrorKey:errorReason}];
  if (hasListeners) {
    NSDictionary *jsError = RCTJSErrorFromCodeMessageAndNSError(@"E_AD_FAILED_TO_LOAD", error.localizedDescription, error);
    [self sendEventWithName:kEventAdFailedToLoad body:jsError];
  }
  
  _requestAdReject(@"E_AD_FAILED_TO_LOAD", error.localizedDescription, error);
  
}

- (void)ad:(ALAd *)ad wasDisplayedIn:(UIView *)view {
  if (hasListeners) {
    [self sendEventWithName:kEventAdOpened body:nil];
  }
}

- (void)ad:(ALAd *)ad wasHiddenIn:(UIView *)view {
    isReady = false;
    if (hasListeners) {
        [self sendEventWithName:kEventAdClosed body:nil];
    }
}

- (void)ad:(ALAd *)ad wasClickedIn:(UIView *)view {

}

- (void)videoPlaybackBeganInAd:(ALAd *)ad {

}

- (void)videoPlaybackEndedInAd:(ALAd *)ad atPlaybackPercent:(NSNumber *)percentPlayed fullyWatched:(BOOL)wasFullyWatched {
}

@end
