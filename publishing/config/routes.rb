Rails.application.routes.draw do

  match 'select_course',  to: 'courses#select', via: :post

  match 'publish_course', to: 'publishing#publish', via: :post
  match 'check_course', to: 'publishing#check', via: :get
  match 'list_jobs', to: 'publishing#jobs', via: :get
  match 'delete_job', to: 'publishing#delete_job', via: :delete

  match 'check_content', to: 'checking#index', via: :get

  #match 'entry_quiz', to: 'entry_quiz#show', via: :get

  resource :entry_quiz do
    resources :entry_quiz_questions, as: :questions do
      member do
        post 'activate'
        post 'deactivate'
        post 'toggle_activation'
        post 'moveup'
        post 'movedown'
        get 'preview'
      end
    end
  end

  resources :chapters do
    member do
      post 'activate'
      post 'deactivate'
      post 'moveup'
      post 'movedown'
      post 'toggle_activation'
      post 'toggle_remedial'
    end

    resources :sections do
      member do
        post 'activate'
        post 'deactivate'
        post 'moveup'
        post 'movedown'
        post 'toggle_activation'
      end
      resources :subsections do
        collection do
          get 'preview'
          post 'save'
        end
      end
      resources :questions do
        member do
          post 'activate'
          post 'deactivate'
          post 'toggle_activation'
          post 'moveup'
          post 'movedown'
          get 'preview'
        end
      end
    end
  end

  resources :inputs do
    resources :answers
    resources :choices
  end

  match '/health-check', to: 'home#health_check', via: :get

  root to: 'home#index'
end
