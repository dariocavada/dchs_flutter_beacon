//
//  FBRangingStreamHandler.h
//  flutter_beacon
//
//  Created by Alann Maulana on 23/01/19.
//

#import <Foundation/Foundation.h>
#import <Flutter/Flutter.h>

NS_ASSUME_NONNULL_BEGIN

@class DchsFlutterBeaconPlugin;
@interface FBRangingStreamHandler : NSObject<FlutterStreamHandler>

@property (strong, nonatomic) DchsFlutterBeaconPlugin* instance;

- (instancetype) initWithFlutterBeaconPlugin:(DchsFlutterBeaconPlugin*) instance;

@end

NS_ASSUME_NONNULL_END
