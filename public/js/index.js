
var mode = "home";

var topTLElement;
var topReplyElement;


var insertNewerTweet = function() {
  getTimelineJSON(function(data) {
    var topTweetId;
    if (topTLElement == undefined) topTweetId = -1;
    else topTweetId = topTLElement.tweet.id;
    for (var oldPoint = 0; oldPoint < data.length; oldPoint++) {
      var tweetId = data[oldPoint].tweet.id;
      if (tweetId == topTweetId) break;
    }
    for (oldPoint--; oldPoint != -1; oldPoint--) {
      var domTweet = createTweet(data[oldPoint].tweet, data[oldPoint].user, data[oldPoint].myId)
      $('#timeline').prepend(domTweet);
    }
    topTLElement = data[0];
  });
}

var insertNewerReply = function() {
  getReplyJSON(function(data) {
    var topTweetId;
    if (topTLElement == undefined) topTweetId = -1;
    else topTweetId = topReplyElement.tweet.id;
    for (var oldPoint = 0; oldPoint < data.length; oldPoint++) {
      var tweetId = data[oldPoint].tweet.id;
      if (tweetId == topTweetId) break;
    }
    for (oldPoint--; oldPoint != -1; oldPoint--) {
      var domTweet = createTweet(data[oldPoint].tweet, data[oldPoint].user, data[oldPoint].myId)
      $('#timeline').prepend(domTweet);
    }
    topReplyElement = data[0];
  });
}


var timelineIntervalHandler;
var replyIntervalHandler;

$('#home').click(function () {
  if (mode != "home") {
    deleteTimeline();
    createLoader();
    mode = "home";
    setButtonToSelected('home');
    if (replyIntervalHandler != null) {
      clearInterval(replyIntervalHandler);
    }
    setTimeout(function() {
      getTimelineJSON(function(data) {
        topTLElement = data[0];
        timelineIntervalHandler = setInterval(function() {
          insertNewerTweet();
        }, 5 * 1000);
        deleteTimeline();
        createTimeline(data, function (event, lastId) {
          loadTimeline(lastId);
        });
      });
    }, 500);
  }
});

$('#reply').click(function () {
  if (mode != "reply") {
    deleteTimeline();
    createLoader();
    mode = "reply";
    setButtonToSelected('reply');
    if (timelineIntervalHandler != null) {
      clearInterval(timelineIntervalHandler);
    }
    setTimeout(function() {
      getReplyJSON(function(data) {
        topReplyElement = data[0];
        replyIntervalHandler = setInterval(function() {
          insertNewerReply();
        }, 5 * 1000);
        deleteTimeline();
        createTimeline(data, function (event, lastId) {
          loadReply(lastId);
        });
      });
    }, 500);
  }
});

var setButtonToSelected = function (button) {
  //var buttonList = {
  //  'home':{notselected: 'assets/img/button_home.png', selected: 'assets/img/button_selected_home.png'},
  //  'reply':{notselected: 'assets/img/button_reply.png', selected: 'assets/img/button_selected_reply.png'}
  //};
  //var menuImgs = $('.menu-img');
  //for (var i = 0; i < menuImgs.length; i++) {
  //  if ($(menuImgs[i]).attr('id') == 'logout') continue;
  //  if ($(menuImgs[i]).attr('id') == 'tweet') continue;
  //  if ($(menuImgs[i]).attr('id') == 'user') continue;
  //  $('img', menuImgs[i]).attr('src', buttonList[menuImgs[i].id].notselected);
  //}
  //$('.menu-img#' + button + ' > img').attr('src', buttonList[button].selected);
  $('.menu-img').removeClass("selected");
  $('.menu-img#' + button).addClass("selected");
}


var createTweet = function(tweet, user, myId) {
  var favClass = '';
  if (tweet.favorited_user_id.indexOf(myId) != -1) {
    favClass = 'tweet-favorited';
  }
  var thisTweet = $('<section></section>', {addClass: 'timeline-tweet'})
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
            )
            .append(
              function () {
                if (tweet.user_id == myId) {
                  return $('<i></i>', {
                    addClass: 'fa fa-trash', on: {
                      click: function (event) {
                        deleteTweet(function (data) {
                          $(thisTweet).remove();
                          console.log(data);
                        }, tweet.id);
                      }
                    }
                  });
                }
              }()
            )
        )
    );
  return thisTweet;
}

var createLoader = function () {
  $('#timeline').append(
    $('<div></div>', {addClass: 'loading'})
      .append($('<img>', {src: '/assets/img/load.gif', alt: 'loading...'}))
    );
}

var deleteTweet = function (callback, tweetId) {
  var url = '/api/delete/' + tweetId;
  $.ajax({
    type: 'DELETE',
    url: url,
    dataType: 'json',
    success: function(data) {
      callback(data);
    }
  });
};

var getTimelineJSON = function (callback, lastId) {
  var url = '/api/timeline';
  if (lastId != undefined) {
    url = url + '/' + lastId;
  }
  $.ajax({
    type: 'GET',
    url: url, 
    dataType: 'json',
    success: function(data) {
      callback(data);
    }
  });
};

var getReplyJSON = function (callback, lastId) {
  var url = '/api/reply';
  if (lastId != undefined) {
    url = url + '/' + lastId;
  }
  $.ajax({
    type: 'GET',
    url: url, 
    dataType: 'json',
    success: function(data) {
      callback(data);
    }
  });
};

var getGroupTimelineJSON = function (callback, groupId, lastId) {
  var url = '/api/grouptimeline/' + groupId;
  if (lastId != undefined) {
    url = url + '/' + lastId;
  }
  $.ajax({
    type: 'GET',
    url: url, 
    dataType: 'json',
    success: function(data) {
      callback(data);
    }
  });
};



var createTimeline = function (data, callback) {
  for (var i = 0; i < data.length; i++) {
    var domTweet = createTweet(data[i].tweet, data[i].user, data[i].myId)
    $('#timeline').append(domTweet);
  }
  var clickFunction = function(lastId) {
    return function(event) {
      $($('#timeline > section')[$('#timeline > section').length - 1]).remove()
      callback(event, lastId)
    };
  }(data[data.length - 1].tweet.id);

  $('#timeline').append(
    $('<section></section>', {addClass: 'timeline-tweet'})
    .append(
      $('<div></div>', {addClass: 'tweet-main', on:{click: clickFunction}})
      .text('↓')
    )
  );
}

var createGroupTimeline = function (data, callback) {
  for (var i = 0; i < data.length; i++) {
    var domTweet = createTweet(data[i].tweet, data[i].user, data[i].myId)
    $('#conversationTimeline').append(domTweet);
  }
  var clickFunction = function(lastId) {
    return function(event) {
      $($('#conversationTimeline > section')[$('#conversationTimeline > section').length - 1]).remove()
      callback(event, lastId)
    };
  }(data[data.length - 1].tweet.id);

  $('#conversationTimeline').append(
    $('<section></section>', {addClass: 'timeline-tweet'})
    .append(
      $('<div></div>', {addClass: 'tweet-main', on:{click: clickFunction}})
      .text('↓')
    )
  );
}


var deleteTimeline = function () {
  $('#timeline').html('');
}

var loadTimeline = function (lastId) {
  var isNew = false;
  if (lastId == undefined) {
    isNew = true;
  }
  getTimelineJSON(function(data, lastId) {
    if (isNew) {
      topTLElement = data[0];
    }
    createTimeline(data, function (event, lastId) {
      loadTimeline(lastId);
    });
  }, lastId);
};

var loadReply = function (lastId) {
  var isNew = false;
  if (lastId == undefined) {
    isNew = true;
  }
  getReplyJSON(function(data, lastId) {
    if (isNew) {
      topReplyElement = data[0];
    }
    createTimeline(data, function (event, lastId) {
      loadReply(lastId);
    });
  }, lastId);
};

var loadGroupTimeline = function (groupId, lastId) {
  getGroupTimelineJSON(function(data, lastId) {
    createGroupTimeline(data, function (event, lastId) {
      loadGroupTimeline(groupId, lastId);
    });
  }, groupId, lastId);
};


/*$('.conversation-index').click(function() {
  var f = $(this);
  var groupId = f.attr('id').split('-')[1];
  getGroupTimelineJSON(function(data,lastId) {
    $('.conversation-detail')
      .html("")
      .click(function() {
        $('.conversation-detail').css('left', '100%');
      })
      .append(
        $('<div></div>', {addClass: 'conversation-title', id: 'conversationTitle'})
          .html('<i class="fa fa-times fa-lg"></i> 話題：' + f.attr('data-index'))
      )
      .append(
        $('<div></div>', {addClass: 'conversation-timeline', id: 'conversationTimeline', on:{click: function(event) {
          event.stopPropagation();
        }}})
      )
    $('.conversation-detail').css('left', 0);
    createGroupTimeline(data,function (event, lastId) {
      loadGroupTimeline(groupId, lastId);
    });
  }, groupId);
});*/


loadTimeline();

timelineIntervalHandler = setInterval(function() {
  insertNewerTweet();
}, 5 * 1000);

