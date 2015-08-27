
var mode = "home";

var topTLElement;


var insertNewerTweet = function(groupId) {
  getIndexJSON(function(data) {
    $('#conversationsTitle').text('話題：' + data);
    getGroupTimelineJSON(function(data) {
      var topTweetId;
      if (topTLElement == undefined) topTweetId = -1;
      else topTweetId = topTLElement.tweet.id;
      for (var oldPoint = 0; oldPoint < data.length; oldPoint++) {
        var tweetId = data[oldPoint].tweet.id;
        if (tweetId == topTweetId) break;
      }
      for (oldPoint--; oldPoint != -1; oldPoint--) {
        var domTweet = createTweet(data[oldPoint].tweet, data[oldPoint].user, data[oldPoint].myId)
        $('#timeline-' + groupId).prepend(domTweet);
      }
      topTLElement = data[0];
    }, groupId);
  },groupId);
}


var timelineIntervalHandler;

/*
var setButtonToSelected = function (button) {
  $('.menu-img').removeClass("selected");
  $('.menu-img#' + button).addClass("selected");
}
*/


var createTweet = function(tweet, user, myId) {
  var favClass = '';
  if (tweet.favorited_user_id.indexOf(myId) != -1) {
    favClass = 'tweet-favorited';
  }
  return $('<section></section>', {addClass: 'timeline-tweet'})
    .append(
      $('<div></div>', {addClass: 'tweet-main'})
        .append(
          $('<div></div>', {addClass: 'tweet-img'})
          .append(
            $('<a></a>', {href: '/' + user.screen_name})
            .append(
              $('<img>', {src: '/assets/' + user.profile_image_url, alt: user.name + "'s icon"})
            )
          )
        )
        .append(
          $('<footer></footer>', {addClass: 'tweet-footer'})
          .append(
              $('<a></a>', {href: '/' + user.screen_name})
            .append(
              $('<div></div>', {addClass: 'tweet-username'})
                .text(user.name)
            )
            .append(
              $('<div></div>', {addClass: 'tweet-screenname'})
                .text('@' + user.screen_name)
            )
          )
          .append(
            $('<div></div>', {addClass: 'tweet-diffdate'})
              .text(tweet.created_at)
          )
        )
        .append(
          $('<p></p>', {addClass: 'tweet-text'})
            .text(tweet.text)
        )
        .append(
          $('<div></div>', {addClass: 'tweet-actions'})
            .append(
              $('<i></i>', {addClass: 'fa fa-reply', on: {click: function(event) {
                $('#tweetInput').val('@' + user.screen_name + " ");
                $('#tweetModal').modal();
              }}})
            )
            .append(
              $('<i></i>', {addClass: 'fa fa-star ' + favClass, on: {click: function(event) {
                var f = $(this)
                var url;
                if (f.hasClass('tweet-favorited')) {
                  url = '/api/removeFavorite/' + tweet.id;
                } else {
                  url = '/api/addFavorite/' + tweet.id;
                }
                $.ajax({
                  typw: 'GET',
                  url: url, 
                  dataType: 'json',
                  success: function(f) {
                    return function(data) {
                      console.log(data);
                      f.toggleClass('tweet-favorited');
                    };
                  }(f)
                });
              }}})
            )
            .append(
              $('<i></i>', {addClass: 'fa fa-retweet', on: {click: function(event) {
                $('#tweetInput').val('RT @' + user.screen_name + ": " + tweet.text);
                $('#tweetModal').modal();
              }}})
            )/*
              .append(
              $('<i></i>', {addClass: 'fa fa-trash-o', on: {click: function(event) {
              }}})
          )*/
        )
    );
}

var getGroupTimelineJSON = function (callback, groupId, lastId) {
  var url = '/api/grouptimeline/' + groupId;
  if (lastId != undefined) {
    url = url + '/' + lastId;
  }
  $.ajax({
    typw: 'GET',
    url: url, 
    dataType: 'json',
    success: function(data) {
      callback(data);
    }
  });
};

var getIndexJSON = function (callback, groupId) {
  var url = '/api/getindex/' + groupId;
  $.ajax({
    typw: 'GET',
    url: url, 
    dataType: 'json',
    success: function(data) {
      callback(data);
    }
  });
};




var createTimeline = function (groupId, data, callback) {
  for (var i = 0; i < data.length; i++) {
    var domTweet = createTweet(data[i].tweet, data[i].user, data[i].myId)
    $('#timeline-' + groupId).append(domTweet);
  }
  var clickFunction = function(lastId) {
    return function(event) {
      $($('#timeline-' + groupId + ' > section')[$('#timeline-' + groupId + ' > section').length - 1]).remove()
      callback(event, lastId)
    };
  }(data[data.length - 1].tweet.id);

  $('#timeline-' + groupId).append(
    $('<section></section>', {addClass: 'timeline-tweet'})
    .append(
      $('<div></div>', {addClass: 'tweet-main', on:{click: clickFunction}})
      .text('↓')
    )
  );
}


var loadTimeline = function (groupId, lastId) {
  var isNew = false;
  if (lastId == undefined) {
    isNew = true;
  }
  getIndexJSON(function(data) {
    $('#conversationsTitle').text('話題：' + data);
    getGroupTimelineJSON(function(data, lastId) {
      if (isNew) {
        topTLElement = data[0];
      }
      createTimeline(groupId, data, function (event, lastId) {
        loadTimeline(groupId, lastId);
      });
    }, groupId, lastId);
  }, groupId);
};


var groupId = $('.timeline').attr('id').split('-')[1];
loadTimeline(groupId);

timelineIntervalHandler = setInterval(function() {
  insertNewerTweet(groupId);
}, 5 * 1000);

