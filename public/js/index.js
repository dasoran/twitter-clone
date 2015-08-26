
var mode = "home";

$('#home').click(function () {
  if (mode != "home") {
    deleteTimeline();
    createLoader();
    mode = "home";
    setButtonToSelected('home');
    setTimeout(function() {
      getTimelineJSON(function(data) {
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
    setTimeout(function() {
      getReplyJSON(function(data) {
        deleteTimeline();
        createTimeline(data, function (event, lastId) {
          loadReply(lastId);
        });
      });
    }, 500);
  }
});

var setButtonToSelected = function (button) {
  var buttonList = {
    'home':{notselected: 'assets/img/button_home.png', selected: 'assets/img/button_selected_home.png'},
    'reply':{notselected: 'assets/img/button_reply.png', selected: 'assets/img/button_selected_reply.png'}
  };
  var menuImgs = $('.menu-img');
  for (var i = 0; i < menuImgs.length; i++) {
    if ($(menuImgs[i]).attr('id') == 'logout') continue;
    $('img', menuImgs[i]).attr('src', buttonList[menuImgs[i].id].notselected);
  }
  $('.menu-img#' + button + ' > img').attr('src', buttonList[button].selected);
}


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
            $('<div></div>', {addClass: 'tweet-username'})
              .text(user.name)
          )
          .append(
            $('<div></div>', {addClass: 'tweet-screenname'})
              .text('@' + user.screen_name)
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
        )
    );
}

var createLoader = function () {
  $('#timeline').append(
    $('<div></div>', {addClass: 'loading'})
      .append($('<img>', {src: '/assets/img/load.gif', alt: 'loading...'}))
    );
}

var getTimelineJSON = function (callback, lastId) {
  var url = '/api/timeline';
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

var getReplyJSON = function (callback, lastId) {
  var url = '/api/reply';
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
  getTimelineJSON(function(data, lastId) {
    createTimeline(data, function (event, lastId) {
      loadTimeline(lastId);
    });
  }, lastId);
};

var loadReply = function (lastId) {
  getReplyJSON(function(data, lastId) {
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


$('.conversation-index').click(function() {
  var f = $(this);
  var groupId = f.attr('id').split('-')[1];
  getGroupTimelineJSON(function(data,lastId) {
    $('.index-main-container').append(
      $('<div></div>', {addClass: 'conversation-detail', on:{click: function() {
        $('.conversation-detail').css('left', '100%');
        setTimeout(function () {
          $('.conversation-detail').remove();
        }, 1000);
      }}})
        .append(
          $('<div></div>', {addClass: 'conversation-title', id: 'conversationTitle'})
            .text('話題：' + f.attr('data-index'))
        )
        .append(
          $('<div></div>', {addClass: 'conversation-timeline', id: 'conversationTimeline', on:{click: function(event) {
            event.stopPropagation();
          }}})
        )
    );
    setTimeout(function () {
      $('.conversation-detail').css('left', 0);
    }, 300);
    createGroupTimeline(data,function (event, lastId) {
      loadGroupTimeline(groupId, lastId);
    });
  }, groupId);
});


loadTimeline();

