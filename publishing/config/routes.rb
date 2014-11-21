Rails.application.routes.draw do

  match 'select_course',  to: 'courses#select', via: :post

  match 'publish_course', to: 'publishing#publish', via: :post
  match 'check_course', to: 'publishing#check', via: :get
  match 'list_jobs', to: 'publishing#jobs', via: :get
  match 'delete_job', to: 'publishing#delete_job', via: :delete

  match 'check_content', to: 'checking#index', via: :get

  match 'search_section', to: 'sections#search', via: [:get, :post]

  resource :entry_quiz do
    resources :entry_quiz_questions, as: :questions do
      member do
        post 'activate'
        post 'deactivate'
        post 'toggle_activation'
        post 'moveup'
        post 'movedown'
        get 'preview_content'
        get 'error_content'
      end
    end
  end

  resources :chapters do
    resource :chapter_quiz, :only => [:show] do
      member do
        post 'toggle_activation'
      end
      resources :chapter_questions_sets do
        member do
          post 'moveup'
          post 'movedown'
        end
        resources :chapter_quiz_questions, as: :questions do
          member do
            get 'preview_content'
            get 'error_content'
          end
        end
      end
    end

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
          get 'preview_content'
          get 'preview_worked_out_answer'
          get 'error_content'
          get 'error_worked_out_answer'
        end
      end
    end
  end

  resources :inputs do
    resources :answers
    resources :choices do
      member do
        post 'moveup'
        post 'movedown'
      end
    end
  end

  match '/error-check',  to: 'home#error_check', via: :get
  match '/health-check', to: 'home#health_check', via: :get

  root to: 'home#index'
end
