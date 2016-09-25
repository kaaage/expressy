//
// Created by kaaage on 28/05/16.
//

#include <pebble.h>

#define SYNC_BUFFER_SIZE 48

static AppSync sync;

static uint8_t sync_buffer[SYNC_BUFFER_SIZE];

typedef struct {
    uint64_t    sync_set;
    uint64_t    sync_vib;
    uint64_t    sync_missed;
} sync_stats_t;
static sync_stats_t syncStats = {0, 0, 0};
static int syncChangeCount = 0;

static Window *s_window;
static TextLayer *s_text_layer;
static uint32_t num_samples = 1;  // Number of samples per batch/callback
static AccelSamplingRate rate = ACCEL_SAMPLING_10HZ;
static bool sendData = false;
static bool failed = false;
static AppTimer *s_timeout_timer;

static uint8_t buffer[14];
static DictionaryIterator *iter;
static int succ_msgs_count   = 0;
static int failed_msgs_count = 0;

static void inbox_dropped_callback(AppMessageResult reason, void *context);
static void inbox_received_callback(DictionaryIterator *iter, void *context);
static void outbox_sent_callback(DictionaryIterator *iter, void *context);
static void outbox_failed_callback(DictionaryIterator *failed, AppMessageResult reason, void *context);
static void accel_data_handler(AccelData *data, uint32_t num_samples);
static void select_click_handler(ClickRecognizerRef recognizer, void *context);
static void click_config_provider(void *context);

static void inbox_dropped_callback(AppMessageResult reason, void *context)
{
    APP_LOG(APP_LOG_LEVEL_ERROR, "Message dropped!");
}

static void inbox_received_callback(DictionaryIterator *iter, void *context)
{
    APP_LOG(APP_LOG_LEVEL_ERROR, "Message received!");
}

static void outbox_sent_callback(DictionaryIterator *iter, void *context) {
    APP_LOG(APP_LOG_LEVEL_DEBUG, "App Message has been successfully delivered!");
}

static void outbox_failed_callback(DictionaryIterator *failed, AppMessageResult reason, void *context) {
    APP_LOG(APP_LOG_LEVEL_DEBUG, "App Message Failed to Send!");
}

static void sync_error_callback(DictionaryResult dict_error, AppMessageResult error, void * context)
{
    APP_LOG(APP_LOG_LEVEL_DEBUG, "App sync error!");
}

static void sync_tuple_changed_callback(const uint32_t key, const Tuple * new_tuple, const Tuple * old_tuple, void * context)
{
    if (syncChangeCount > 0) {
        syncChangeCount--;
    }
    else {
        APP_LOG(APP_LOG_LEVEL_INFO, "unblock");
    }
}

static void accel_data_handler(AccelData *data, uint32_t num_samples) {
    AppMessageResult result;
    AccelData * vector = (AccelData*) data;

    if (vector->did_vibrate) {
        syncStats.sync_vib++;
        return;
    }

    if (syncChangeCount > 0) {
        syncStats.sync_missed++;
        return;
    }

    // Create the dictionary with vector data
    Tuplet vector_dict[] = {
            TupletInteger(128, 1),
            TupletInteger(1, (int) vector->x),
            TupletInteger(2, (int) vector->y),
            TupletInteger(3, (int) vector->z),
    };

    // Send dictionary to the smartphone app
    result = app_sync_set( &sync, vector_dict, ARRAY_LENGTH(vector_dict) );

    if (result != APP_MSG_OK) {
        APP_LOG(APP_LOG_LEVEL_DEBUG, "app_sync_set: APP_MSG_%i", result);
    }
    else {
        syncChangeCount = ARRAY_LENGTH(vector_dict);
        syncStats.sync_set++;
    }
}

static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
    if (!sendData) {
        sendData = true;
        text_layer_set_text(s_text_layer, "Sending data...");

        // Subscribe to batched data events
        accel_data_service_subscribe(num_samples, accel_data_handler);
        APP_LOG(APP_LOG_LEVEL_DEBUG, "Subscribed!");
    }
    else {
        sendData = false;
        text_layer_set_text(s_text_layer, "Idle...");

        // Subscribe to batched data events
        accel_data_service_unsubscribe();
        APP_LOG(APP_LOG_LEVEL_DEBUG, "Unsubscribed!");
    }
}

static void click_config_provider(void *context) {
    window_single_click_subscribe(BUTTON_ID_SELECT, select_click_handler);
}

static void window_load(Window * window)
{
    Tuplet vector_dict[] = {
            TupletInteger(128, 0),
            TupletInteger(1, (int) 0x11111111),
            TupletInteger(2, (int) 0x22222222),
            TupletInteger(3, (int) 0x33333333),
    };

    syncChangeCount = ARRAY_LENGTH(vector_dict);

    app_sync_init( &sync, sync_buffer, sizeof(sync_buffer), vector_dict, ARRAY_LENGTH(vector_dict), sync_tuple_changed_callback, sync_error_callback, NULL);

    syncStats.sync_set++;
}

static void window_unload(Window * window)
{
    APP_LOG(APP_LOG_LEVEL_INFO, "%s", __FUNCTION__);

    app_sync_deinit(&sync);
}

static void init(void) {
    // Register callbacks
//    app_message_register_inbox_received(inbox_received_callback);
//    app_message_register_inbox_dropped(inbox_dropped_callback);
//    app_message_register_outbox_failed(outbox_failed_callback);
//    app_message_register_outbox_sent(outbox_sent_callback);

    // Init buffers
    app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum());

//    app_message_outbox_begin(&iter);
    app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED); // faster bluetooth interval -> minimize timeouts
    accel_service_set_sampling_rate(rate);

    // Create a window and get information about the window
    s_window = window_create();
    WindowHandlers handlers = {.load = window_load, .unload = window_unload };
    window_set_window_handlers(s_window, handlers);
    Layer *window_layer = window_get_root_layer(s_window);
    GRect bounds = layer_get_bounds(window_layer);

    // Create a text layer and set the text
    s_text_layer = text_layer_create(bounds);
    text_layer_set_text(s_text_layer, "Idle...");

    // Set the font and text alignment
    text_layer_set_font(s_text_layer, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
    text_layer_set_text_alignment(s_text_layer, GTextAlignmentCenter);

    // Add the text layer to the window
    layer_add_child(window_get_root_layer(s_window), text_layer_get_layer(s_text_layer));

    // Enable text flow and paging on the text layer, with a slight inset of 10, for round screens
    text_layer_enable_screen_text_flow_and_paging(s_text_layer, 10);

    // Push the window, setting the window animation to 'true'
    window_stack_push(s_window, true);

    window_set_click_config_provider(s_window, click_config_provider);
}

static void deinit(void) {
    app_comm_set_sniff_interval(SNIFF_INTERVAL_NORMAL);

    // Destroy the text layer
    text_layer_destroy(s_text_layer);

    // Destroy the window
    window_destroy(s_window);
}

int main(void) {
    init();
    app_event_loop();
    deinit();
}
